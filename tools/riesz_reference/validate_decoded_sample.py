"""Validate Riesz reference metrics on decoded sample-video luminance frames."""

from __future__ import annotations

from dataclasses import dataclass
import argparse
import json
import math
from pathlib import Path
from typing import Sequence

from riesz_reference import compare_linear_and_phase


Image = list[list[float]]


@dataclass(frozen=True)
class DecodedSampleMetrics:
    source: str
    frame_count: int
    pair_count: int
    mean_linear_delta: float
    mean_phase_delta: float
    mean_phase_to_linear_ratio: float
    mean_linear_roughness: float
    mean_phase_roughness: float

    @property
    def passed(self) -> bool:
        return (
            self.frame_count >= 20
            and self.pair_count > 0
            and math.isfinite(self.mean_linear_delta)
            and math.isfinite(self.mean_phase_delta)
            and self.mean_linear_delta > 0.0
            and self.mean_phase_delta > 0.0
            and math.isfinite(self.mean_phase_to_linear_ratio)
            and math.isfinite(self.mean_linear_roughness)
            and math.isfinite(self.mean_phase_roughness)
        )

    def summary(self) -> str:
        status = "PASS" if self.passed else "FAIL"
        return (
            f"{status} {self.source}: {self.frame_count} frames, {self.pair_count} pairs, "
            f"linear_delta={self.mean_linear_delta:.6f}, "
            f"phase_delta={self.mean_phase_delta:.6f}, "
            f"ratio={self.mean_phase_to_linear_ratio:.6f}, "
            f"linear_roughness={self.mean_linear_roughness:.6f}, "
            f"phase_roughness={self.mean_phase_roughness:.6f}"
        )


def validate_decoded_sample(path: Path, amplification: float = 4.0) -> DecodedSampleMetrics:
    payload = json.loads(path.read_text())
    frames = payload.get("frames", [])
    if not isinstance(frames, list):
        raise ValueError("decoded sample JSON must contain a frames array")
    images = [validate_image(frame) for frame in frames]

    linear_deltas: list[float] = []
    phase_deltas: list[float] = []
    ratios: list[float] = []
    linear_roughness: list[float] = []
    phase_roughness: list[float] = []

    for previous, current in zip(images, images[1:]):
        metrics = compare_linear_and_phase(previous, current, amplification)
        linear_deltas.append(metrics.linear_mean_abs_delta)
        phase_deltas.append(metrics.phase_mean_abs_delta)
        ratios.append(metrics.phase_to_linear_delta_ratio)
        linear_roughness.append(metrics.linear_roughness)
        phase_roughness.append(metrics.phase_roughness)

    return DecodedSampleMetrics(
        source=str(payload.get("source", path.name)),
        frame_count=len(images),
        pair_count=len(linear_deltas),
        mean_linear_delta=mean(linear_deltas),
        mean_phase_delta=mean(phase_deltas),
        mean_phase_to_linear_ratio=mean(ratios),
        mean_linear_roughness=mean(linear_roughness),
        mean_phase_roughness=mean(phase_roughness),
    )


def validate_image(frame: object) -> Image:
    if not isinstance(frame, list) or not frame:
        raise ValueError("each frame must be a non-empty 2D array")
    width = None
    image: Image = []
    for row in frame:
        if not isinstance(row, list) or not row:
            raise ValueError("each frame row must be a non-empty array")
        if width is None:
            width = len(row)
        elif len(row) != width:
            raise ValueError("frame rows must have equal width")
        image.append([float(value) for value in row])
    return image


def mean(values: Sequence[float]) -> float:
    return sum(values) / float(len(values)) if values else 0.0


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("json_path", type=Path)
    parser.add_argument("--amplification", type=float, default=4.0)
    args = parser.parse_args()
    result = validate_decoded_sample(args.json_path, amplification=args.amplification)
    print(result.summary())
    if not result.passed:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
