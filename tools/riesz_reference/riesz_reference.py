"""Dependency-free reference Riesz pyramid helpers.

This module favors readable, deterministic operations over speed. It is a
reference for validating phase-mode behavior before porting the math to C++ or
GPU shaders.
"""

from __future__ import annotations

from dataclasses import dataclass
import math
from typing import List, Sequence, Tuple


Image = List[List[float]]
Kernel3x3 = Tuple[Tuple[float, float, float], Tuple[float, float, float], Tuple[float, float, float]]


_GAUSSIAN_3X3: Kernel3x3 = (
    (1.0 / 16.0, 2.0 / 16.0, 1.0 / 16.0),
    (2.0 / 16.0, 4.0 / 16.0, 2.0 / 16.0),
    (1.0 / 16.0, 2.0 / 16.0, 1.0 / 16.0),
)

_RIESZ_X_3X3: Kernel3x3 = (
    (0.0, 0.0, 0.0),
    (-0.5, 0.0, 0.5),
    (0.0, 0.0, 0.0),
)

_RIESZ_Y_3X3: Kernel3x3 = (
    (0.0, -0.5, 0.0),
    (0.0, 0.0, 0.0),
    (0.0, 0.5, 0.0),
)


@dataclass(frozen=True)
class RieszLevel:
    """One Gaussian pyramid level and its first-order Riesz components."""

    image: Image
    riesz_x: Image
    riesz_y: Image

    @property
    def size(self) -> Tuple[int, int]:
        return image_size(self.image)


@dataclass(frozen=True)
class PhaseLevel:
    """Dominant-orientation phase representation for one Riesz level."""

    orientation_radians: float
    oriented_riesz: Image
    amplitude: Image
    phase: Image


@dataclass(frozen=True)
class ComparisonMetrics:
    """Summary metrics for comparing simple EVM and phase reference output."""

    linear_mean_abs_delta: float
    phase_mean_abs_delta: float
    linear_roughness: float
    phase_roughness: float

    @property
    def phase_to_linear_delta_ratio(self) -> float:
        if self.linear_mean_abs_delta == 0.0:
            return math.inf if self.phase_mean_abs_delta > 0.0 else 0.0
        return self.phase_mean_abs_delta / self.linear_mean_abs_delta


def image_size(image: Sequence[Sequence[float]]) -> Tuple[int, int]:
    validate_image(image)
    return (len(image[0]), len(image))


def validate_image(image: Sequence[Sequence[float]]) -> None:
    if not image:
        raise ValueError("image must contain at least one row")
    if not image[0]:
        raise ValueError("image rows must contain at least one value")

    width = len(image[0])
    for row in image:
        if len(row) != width:
            raise ValueError("image rows must all have the same width")


def build_gaussian_pyramid(image: Sequence[Sequence[float]], levels: int) -> List[Image]:
    if levels <= 0:
        raise ValueError("levels must be positive")

    current = clone_image(image)
    pyramid = [current]
    while len(pyramid) < levels and image_size(current) != (1, 1):
        current = downsample2(current)
        pyramid.append(current)
    return pyramid


def build_riesz_pyramid(image: Sequence[Sequence[float]], levels: int) -> List[RieszLevel]:
    return [
        RieszLevel(
            image=level,
            riesz_x=convolve_3x3(level, _RIESZ_X_3X3),
            riesz_y=convolve_3x3(level, _RIESZ_Y_3X3),
        )
        for level in build_gaussian_pyramid(image, levels)
    ]


def dominant_orientation(level: RieszLevel) -> float:
    """Return the strongest global orientation for a Riesz level in radians."""

    validate_same_size(level.riesz_x, level.riesz_y)
    sum_x = sum(float(value) for row in level.riesz_x for value in row)
    sum_y = sum(float(value) for row in level.riesz_y for value in row)
    if abs(sum_x) < 1e-12 and abs(sum_y) < 1e-12:
        return 0.0
    return math.atan2(sum_y, sum_x)


def project_phase(level: RieszLevel, orientation_radians: float | None = None) -> PhaseLevel:
    """Project Riesz components onto an orientation and compute local phase."""

    validate_same_size(level.image, level.riesz_x)
    validate_same_size(level.image, level.riesz_y)

    orientation = dominant_orientation(level) if orientation_radians is None else orientation_radians
    cos_theta = math.cos(orientation)
    sin_theta = math.sin(orientation)
    oriented: Image = []
    amplitude: Image = []
    phase: Image = []

    for y, row in enumerate(level.image):
        oriented_row: List[float] = []
        amplitude_row: List[float] = []
        phase_row: List[float] = []
        for x, value in enumerate(row):
            riesz_value = level.riesz_x[y][x] * cos_theta + level.riesz_y[y][x] * sin_theta
            source_value = float(value)
            oriented_row.append(riesz_value)
            amplitude_row.append(math.hypot(source_value, riesz_value))
            phase_row.append(math.atan2(riesz_value, source_value))
        oriented.append(oriented_row)
        amplitude.append(amplitude_row)
        phase.append(phase_row)

    return PhaseLevel(
        orientation_radians=orientation,
        oriented_riesz=oriented,
        amplitude=amplitude,
        phase=phase,
    )


def phase_delta(reference_phase: Sequence[Sequence[float]], current_phase: Sequence[Sequence[float]]) -> Image:
    validate_same_size(reference_phase, current_phase)
    return [
        [wrap_phase(float(current_phase[y][x]) - float(reference_phase[y][x])) for x in range(len(row))]
        for y, row in enumerate(reference_phase)
    ]


def amplify_phase(
    reference_phase: Sequence[Sequence[float]],
    current_phase: Sequence[Sequence[float]],
    amplification: float,
) -> Image:
    delta = phase_delta(reference_phase, current_phase)
    return [
        [wrap_phase(float(reference_phase[y][x]) + delta[y][x] * amplification) for x in range(len(row))]
        for y, row in enumerate(reference_phase)
    ]


def reconstruct_from_phase(phase_level: PhaseLevel, phase: Sequence[Sequence[float]]) -> Image:
    validate_same_size(phase_level.amplitude, phase)
    return [
        [phase_level.amplitude[y][x] * math.cos(float(phase[y][x])) for x in range(len(row))]
        for y, row in enumerate(phase)
    ]


def linear_evm_frame(
    reference_image: Sequence[Sequence[float]],
    current_image: Sequence[Sequence[float]],
    amplification: float,
) -> Image:
    validate_same_size(reference_image, current_image)
    return [
        [
            float(reference_image[y][x]) +
            (float(current_image[y][x]) - float(reference_image[y][x])) * amplification
            for x in range(len(row))
        ]
        for y, row in enumerate(reference_image)
    ]


def phase_magnified_frame(
    reference_image: Sequence[Sequence[float]],
    current_image: Sequence[Sequence[float]],
    amplification: float,
) -> Image:
    reference_level = build_riesz_pyramid(reference_image, levels=1)[0]
    current_level = build_riesz_pyramid(current_image, levels=1)[0]
    orientation = dominant_orientation(current_level)
    reference_phase = project_phase(reference_level, orientation)
    current_phase = project_phase(current_level, orientation)
    amplified_phase = amplify_phase(reference_phase.phase, current_phase.phase, amplification)
    return reconstruct_from_phase(current_phase, amplified_phase)


def compare_linear_and_phase(
    reference_image: Sequence[Sequence[float]],
    current_image: Sequence[Sequence[float]],
    amplification: float,
) -> ComparisonMetrics:
    linear = linear_evm_frame(reference_image, current_image, amplification)
    phase = phase_magnified_frame(reference_image, current_image, amplification)
    return ComparisonMetrics(
        linear_mean_abs_delta=mean_abs_difference(reference_image, linear),
        phase_mean_abs_delta=mean_abs_difference(reference_image, phase),
        linear_roughness=image_roughness(linear),
        phase_roughness=image_roughness(phase),
    )


def smooth_phase3(phase: Sequence[Sequence[float]]) -> Image:
    """Circularly smooth wrapped phase values with the reference 3x3 kernel."""

    validate_image(phase)
    sin_smoothed = smooth3([[math.sin(float(value)) for value in row] for row in phase])
    cos_smoothed = smooth3([[math.cos(float(value)) for value in row] for row in phase])
    return [
        [math.atan2(sin_smoothed[y][x], cos_smoothed[y][x]) for x in range(len(row))]
        for y, row in enumerate(phase)
    ]


def downsample2(image: Sequence[Sequence[float]]) -> Image:
    smoothed = smooth3(image)
    width, height = image_size(smoothed)
    return [
        [smoothed[y][x] for x in range(0, width, 2)]
        for y in range(0, height, 2)
    ]


def smooth3(image: Sequence[Sequence[float]]) -> Image:
    return convolve_3x3(image, _GAUSSIAN_3X3)


def convolve_3x3(image: Sequence[Sequence[float]], kernel: Kernel3x3) -> Image:
    validate_image(image)
    height = len(image)
    width = len(image[0])

    output: Image = []
    for y in range(height):
        row: List[float] = []
        for x in range(width):
            total = 0.0
            for ky in range(3):
                source_y = clamp(y + ky - 1, 0, height - 1)
                for kx in range(3):
                    source_x = clamp(x + kx - 1, 0, width - 1)
                    total += float(image[source_y][source_x]) * kernel[ky][kx]
            row.append(total)
        output.append(row)
    return output


def component_energy(image: Sequence[Sequence[float]]) -> float:
    validate_image(image)
    return math.sqrt(sum(float(value) * float(value) for row in image for value in row))


def mean_abs_difference(first: Sequence[Sequence[float]], second: Sequence[Sequence[float]]) -> float:
    validate_same_size(first, second)
    width, height = image_size(first)
    total = 0.0
    for y in range(height):
        for x in range(width):
            total += abs(float(second[y][x]) - float(first[y][x]))
    return total / float(width * height)


def image_roughness(image: Sequence[Sequence[float]]) -> float:
    validate_image(image)
    width, height = image_size(image)
    total = 0.0
    count = 0
    for y in range(height):
        for x in range(width):
            value = float(image[y][x])
            if x + 1 < width:
                total += abs(float(image[y][x + 1]) - value)
                count += 1
            if y + 1 < height:
                total += abs(float(image[y + 1][x]) - value)
                count += 1
    return total / float(count) if count else 0.0


def validate_same_size(first: Sequence[Sequence[float]], second: Sequence[Sequence[float]]) -> None:
    if image_size(first) != image_size(second):
        raise ValueError("images must have the same size")


def clone_image(image: Sequence[Sequence[float]]) -> Image:
    validate_image(image)
    return [[float(value) for value in row] for row in image]


def clamp(value: int, lower: int, upper: int) -> int:
    return max(lower, min(value, upper))


def wrap_phase(value: float) -> float:
    return math.atan2(math.sin(value), math.cos(value))
