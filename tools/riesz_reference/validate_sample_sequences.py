"""Validate the Riesz reference against deterministic frame sequences."""

from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Iterable, Sequence

from riesz_reference import (
    build_riesz_pyramid,
    compare_linear_and_phase,
    dominant_orientation,
    wrap_phase,
)


Image = list[list[float]]


@dataclass(frozen=True)
class SampleSequence:
    name: str
    frames: Sequence[Image]
    expected_orientation_radians: float | None
    expects_motion: bool


@dataclass(frozen=True)
class SampleSequenceResult:
    name: str
    frame_count: int
    mean_linear_delta: float
    mean_phase_delta: float
    mean_phase_to_linear_ratio: float
    max_orientation_error: float
    passed: bool

    def summary(self) -> str:
        status = "PASS" if self.passed else "FAIL"
        return (
            f"{status} {self.name}: {self.frame_count} frames, "
            f"linear_delta={self.mean_linear_delta:.6f}, "
            f"phase_delta={self.mean_phase_delta:.6f}, "
            f"ratio={self.mean_phase_to_linear_ratio:.6f}, "
            f"max_orientation_error={self.max_orientation_error:.6f}"
        )


def validate_sample_sequence(
    sequence: SampleSequence,
    amplification: float = 4.0,
    still_delta_threshold: float = 1e-9,
    motion_delta_threshold: float = 0.01,
    orientation_error_threshold: float = 0.2,
) -> SampleSequenceResult:
    if len(sequence.frames) < 2:
        raise ValueError("sequence must contain at least two frames")

    reference = sequence.frames[0]
    linear_deltas: list[float] = []
    phase_deltas: list[float] = []
    ratios: list[float] = []
    orientation_errors: list[float] = []

    for frame in sequence.frames[1:]:
        metrics = compare_linear_and_phase(reference, frame, amplification)
        linear_deltas.append(metrics.linear_mean_abs_delta)
        phase_deltas.append(metrics.phase_mean_abs_delta)
        ratios.append(metrics.phase_to_linear_delta_ratio)
        if sequence.expected_orientation_radians is not None:
            level = build_riesz_pyramid(frame, levels=1)[0]
            orientation_errors.append(
                abs(wrap_phase(dominant_orientation(level) - sequence.expected_orientation_radians))
            )

    mean_linear = mean(linear_deltas)
    mean_phase = mean(phase_deltas)
    mean_ratio = mean(ratios)
    max_orientation_error = max(orientation_errors, default=0.0)

    if sequence.expects_motion:
        passed = (
            mean_linear > motion_delta_threshold
            and mean_phase > motion_delta_threshold
            and max_orientation_error <= orientation_error_threshold
        )
    else:
        passed = mean_linear <= still_delta_threshold and mean_phase <= still_delta_threshold

    return SampleSequenceResult(
        name=sequence.name,
        frame_count=len(sequence.frames),
        mean_linear_delta=mean_linear,
        mean_phase_delta=mean_phase,
        mean_phase_to_linear_ratio=mean_ratio,
        max_orientation_error=max_orientation_error,
        passed=passed,
    )


def validate_known_sequences() -> list[SampleSequenceResult]:
    return [validate_sample_sequence(sequence) for sequence in known_sequences()]


def known_sequences() -> list[SampleSequence]:
    return [
        SampleSequence(
            name="stationary flat field",
            frames=[flat_frame() for _ in range(4)],
            expected_orientation_radians=None,
            expects_motion=False,
        ),
        SampleSequence(
            name="vertical edge translating right",
            frames=[vertical_edge(edge_x=x) for x in (10, 10, 11, 11, 12)],
            expected_orientation_radians=0.0,
            expects_motion=True,
        ),
        SampleSequence(
            name="horizontal edge translating down",
            frames=[horizontal_edge(edge_y=y) for y in (6, 6, 7, 7, 8)],
            expected_orientation_radians=math.pi / 2.0,
            expects_motion=True,
        ),
    ]


def flat_frame(width: int = 24, height: int = 16, value: float = 0.5) -> Image:
    return [[value for _ in range(width)] for _ in range(height)]


def vertical_edge(width: int = 24, height: int = 16, edge_x: int = 10) -> Image:
    return [
        [0.15 if x < edge_x else 0.85 for x in range(width)]
        for _ in range(height)
    ]


def horizontal_edge(width: int = 24, height: int = 16, edge_y: int = 6) -> Image:
    return [
        [0.15 if y < edge_y else 0.85 for _ in range(width)]
        for y in range(height)
    ]


def mean(values: Iterable[float]) -> float:
    values = list(values)
    return sum(values) / float(len(values)) if values else 0.0


def main() -> None:
    results = validate_known_sequences()
    for result in results:
        print(result.summary())
    if not all(result.passed for result in results):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
