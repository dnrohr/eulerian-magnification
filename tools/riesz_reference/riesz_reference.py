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


def clone_image(image: Sequence[Sequence[float]]) -> Image:
    validate_image(image)
    return [[float(value) for value in row] for row in image]


def clamp(value: int, lower: int, upper: int) -> int:
    return max(lower, min(value, upper))
