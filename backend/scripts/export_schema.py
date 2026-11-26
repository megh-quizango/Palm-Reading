"""Write contracts/palm_reading.v1.schema.json from the Pydantic models.

Run: uv run --directory backend python scripts/export_schema.py
"""

import json
import sys
from pathlib import Path

BACKEND = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND))

from app.schemas.palm import PalmReading  # noqa: E402

OUT = BACKEND.parent / "contracts" / "palm_reading.v1.schema.json"


def export() -> dict:
    return {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "palm_reading.v1",
        **PalmReading.model_json_schema(),
    }


if __name__ == "__main__":
    OUT.write_text(json.dumps(export(), indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    print(f"wrote {OUT}")
