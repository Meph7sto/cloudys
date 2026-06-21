from typing import Any, AsyncGenerator, Dict, Generator, List, Optional

import asyncio

from ..config import settings
from .remote import DeepSeekClient, ToolCallResult
from .remote.deepseek import DEFAULT_DEEPSEEK_MODEL
from .remote.async_deepseek import AsyncDeepSeekClient


class DeepSeekEngine:
    """DeepSeek API 模型引擎 — 通过 DeepSeek API 执行文本生成任务。"""

    def __init__(
        self,
        deepseek_api_url: Optional[str] = None,
        deepseek_api_key: Optional[str] = None,
        deepseek_model: Optional[str] = None,
    ):
        self.deepseek_api_url = deepseek_api_url or settings.DEEPSEEK_API_URL
        self.deepseek_api_key = deepseek_api_key or settings.DEEPSEEK_API_KEY
        self.deepseek_model = deepseek_model or getattr(
            settings, "DEEPSEEK_MODEL", DEFAULT_DEEPSEEK_MODEL
        )
        self.model = None
        self.tokenizer = None
        self._sync_client = None
        self._async_client: Optional[AsyncDeepSeekClient] = None

    def _get_sync_client(self) -> DeepSeekClient:
        if self._sync_client is None:
            self._sync_client = DeepSeekClient(
                self.deepseek_api_url,
                self.deepseek_api_key,
                model=self.deepseek_model,
            )
        return self._sync_client

    async def _get_async_client(self) -> AsyncDeepSeekClient:
        if self._async_client is None:
            self._async_client = AsyncDeepSeekClient(
                self.deepseek_api_url,
                self.deepseek_api_key,
                model=self.deepseek_model,
            )
        await self._async_client.ensure_session()
        return self._async_client

    def load_model(self):
        """标记引擎为可用（DeepSeek API 模式下无需加载本地权重）。"""
        self.model = "api"
        self.tokenizer = None
        print("Using DeepSeek API provider, skip local model loading.")

    # ---------------------
    # 同步接口
    # ---------------------

    def generate(
        self,
        prompt: str,
        system_prompt: str,
        max_new_tokens: int = 128,
        model: Optional[str] = None,
        thinking_enabled: bool = True,
    ) -> str:
        client = self._get_sync_client()
        return client.generate(
            prompt, system_prompt, max_new_tokens,
            model_override=model, thinking_enabled=thinking_enabled,
        )

    def generate_stream(
        self,
        prompt: str,
        system_prompt: str,
        max_new_tokens: int = 128,
        model: Optional[str] = None,
        thinking_enabled: bool = True,
    ):
        client = self._get_sync_client()
        return client.generate_stream(
            prompt, system_prompt, max_new_tokens,
            model_override=model, thinking_enabled=thinking_enabled,
        )

    def generate_with_tools(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        max_new_tokens: int = 4096,
        tool_choice: Optional[str] = None,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        thinking_enabled: bool = True,
    ) -> ToolCallResult:
        client = self._get_sync_client()
        return client.generate_with_tools(
            messages=messages, tools=tools, max_new_tokens=max_new_tokens,
            tool_choice=tool_choice, model_override=model,
            temperature=temperature, thinking_enabled=thinking_enabled,
        )

    def generate_with_tools_stream(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        max_new_tokens: int = 4096,
        tool_choice: Optional[str] = None,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        thinking_enabled: bool = True,
    ) -> Generator[Dict[str, Any], None, None]:
        client = self._get_sync_client()
        return client.generate_with_tools_stream(
            messages=messages, tools=tools, max_new_tokens=max_new_tokens,
            tool_choice=tool_choice, model_override=model,
            temperature=temperature, thinking_enabled=thinking_enabled,
        )

    # ---------------------
    # 异步接口
    # ---------------------

    async def generate_async(
        self,
        prompt: str,
        system_prompt: str,
        max_new_tokens: int = 128,
        model: Optional[str] = None,
        thinking_enabled: bool = True,
    ) -> str:
        client = await self._get_async_client()
        return await client.generate(
            prompt, system_prompt, max_new_tokens,
            model_override=model, thinking_enabled=thinking_enabled,
        )

    async def generate_stream_async(
        self,
        prompt: str,
        system_prompt: str,
        max_new_tokens: int = 128,
        model: Optional[str] = None,
        thinking_enabled: bool = True,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        client = await self._get_async_client()
        async for chunk in client.generate_stream(
            prompt, system_prompt, max_new_tokens,
            model_override=model, thinking_enabled=thinking_enabled,
        ):
            yield chunk

    async def close_async(self) -> None:
        if self._async_client is not None:
            await self._async_client.close()
            self._async_client = None

    async def generate_with_tools_async(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        max_new_tokens: int = 4096,
        tool_choice: Optional[str] = None,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        thinking_enabled: bool = True,
    ) -> ToolCallResult:
        client = await self._get_async_client()
        return await client.generate_with_tools(
            messages=messages, tools=tools, max_new_tokens=max_new_tokens,
            tool_choice=tool_choice, model_override=model,
            temperature=temperature, thinking_enabled=thinking_enabled,
        )

    async def generate_with_tools_stream_async(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        max_new_tokens: int = 4096,
        tool_choice: Optional[str] = None,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        thinking_enabled: bool = True,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        client = await self._get_async_client()
        async for chunk in client.generate_with_tools_stream(
            messages=messages, tools=tools, max_new_tokens=max_new_tokens,
            tool_choice=tool_choice, model_override=model,
            temperature=temperature, thinking_enabled=thinking_enabled,
        ):
            yield chunk


__all__ = ["DeepSeekEngine", "ToolCallResult"]
