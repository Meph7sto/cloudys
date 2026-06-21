import asyncio
import logging
import os
import sys
from contextlib import asynccontextmanager
from typing import Callable, Optional
from fastapi import FastAPI

# 确保可以从本地模块导入
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from core.model_container import classification_engine, model_engine
from core.config import settings
from routers import (
    conflict,
    traceability,
    classification,
    requirements_acquisition,
    chat,
)
from routers import kb, l4_generation, l4_validation

# KB Service 导入
from services.kb_service import get_kb_service


logger = logging.getLogger(__name__)


def _is_windows_connection_reset_noise(context: dict) -> bool:
    message = str(context.get("message") or "")
    handle_repr = str(context.get("handle") or "")
    exception = context.get("exception")

    if not isinstance(exception, ConnectionResetError):
        return False
    if getattr(exception, "winerror", None) != 10054:
        return False

    target = "_ProactorBasePipeTransport._call_connection_lost"
    return target in message or target in handle_repr


def _install_windows_asyncio_noise_filter() -> Optional[Callable[[], None]]:
    if sys.platform != "win32":
        return None

    loop = asyncio.get_running_loop()
    previous_handler = loop.get_exception_handler()

    def _handler(active_loop, context):
        if _is_windows_connection_reset_noise(context):
            logger.debug(
                "Suppressed Windows Proactor connection reset noise during transport cleanup: %s",
                context.get("message") or context.get("exception"),
            )
            return
        if previous_handler is not None:
            previous_handler(active_loop, context)
            return
        active_loop.default_exception_handler(context)

    loop.set_exception_handler(_handler)

    def _restore() -> None:
        loop.set_exception_handler(previous_handler)

    return _restore


def _initialize_engines() -> None:
    """初始化推理引擎（DeepSeek API 模式，无需加载本地权重）。"""
    print(f"Initializing inference engine (provider=deepseek, model={settings.DEEPSEEK_MODEL})")
    try:
        model_engine.load_model()
    except Exception as e:
        print(f"CRITICAL ERROR: Failed to initialize model engine: {e}")

    print("Initializing classification engine (provider=deepseek)")
    try:
        classification_engine.load()
    except Exception as e:
        print(f"CRITICAL ERROR: Failed to initialize classification engine: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI 生命周期管理器
    """
    restore_asyncio_handler = _install_windows_asyncio_noise_filter()

    # 启动时：初始化引擎
    _initialize_engines()

    # 加载 KB 索引
    if settings.SKIP_KB_LOAD:
        print("[lifespan] SKIP_KB_LOAD is true, skipping KB index loading.")
        os.environ.setdefault("HF_HUB_OFFLINE", "1")
        os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
    else:
        print("Initializing Knowledge Base indexes...")
        try:
            kb_service = get_kb_service()
            kb_version = kb_service.ensure_loaded()
            print(f"KB indexes loaded successfully, version: {kb_version}")
        except Exception as e:
            print(f"WARNING: Failed to load KB indexes: {e}")

    try:
        yield
    finally:
        if restore_asyncio_handler is not None:
            restore_asyncio_handler()


# 创建 FastAPI 应用实例
app = FastAPI(
    title="Semantic Atlas Inference API",
    version="1.0",
    description="Backend inference service for requirement analysis (DeepSeek API).",
    lifespan=lifespan,
)

from fastapi.middleware.cors import CORSMiddleware

# 添加 CORS 中间件，允许所有来源访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由 (Routers)
app.include_router(conflict.router)
app.include_router(traceability.router)
app.include_router(classification.router)
app.include_router(requirements_acquisition.router)
app.include_router(chat.router)

# L4 需求生成相关路由
app.include_router(kb.router)
app.include_router(l4_generation.router)
app.include_router(l4_validation.router)
app.include_router(l4_generation.root_router)
app.include_router(l4_validation.root_router)


@app.get("/health")
def health_check():
    """
    健康检查接口
    检查引擎是否已初始化
    """
    return {
        "status": "ok",
        "model_loaded": model_engine.model is not None,
        "classifier_loaded": classification_engine.is_ready,
    }


if __name__ == "__main__":
    import uvicorn

    # 启动 uvicorn 服务器
    uvicorn.run(app, host="0.0.0.0", port=8000)
