from pathlib import Path


def test_expected_route_modules_exist():
    root = Path(__file__).resolve().parents[1]
    routers = root / "routers"

    expected = {
        "chat.py",
        "classification.py",
        "conflict.py",
        "kb.py",
        "l4_generation.py",
        "l4_validation.py",
        "requirements_acquisition.py",
        "traceability.py",
    }

    assert routers.exists()
    assert expected.issubset({path.name for path in routers.glob("*.py")})
