import sys
from pathlib import Path
import math
import unittest


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from riesz_reference import (  # noqa: E402
    build_gaussian_pyramid,
    build_riesz_pyramid,
    component_energy,
    amplify_phase,
    dominant_orientation,
    image_size,
    phase_delta,
    project_phase,
    smooth_phase3,
    wrap_phase,
)


class RieszReferenceTest(unittest.TestCase):
    def test_gaussian_pyramid_downsamples_until_requested_level_count(self):
        image = [[float(x + y) for x in range(5)] for y in range(4)]

        pyramid = build_gaussian_pyramid(image, levels=4)

        self.assertEqual([(5, 4), (3, 2), (2, 1), (1, 1)], [image_size(level) for level in pyramid])

    def test_flat_image_has_zero_riesz_response(self):
        image = [[0.25 for _ in range(6)] for _ in range(4)]

        level = build_riesz_pyramid(image, levels=1)[0]

        self.assertAlmostEqual(0.0, component_energy(level.riesz_x), places=7)
        self.assertAlmostEqual(0.0, component_energy(level.riesz_y), places=7)

    def test_horizontal_ramp_has_stronger_x_component(self):
        image = [[float(x) for x in range(8)] for _ in range(6)]

        level = build_riesz_pyramid(image, levels=1)[0]

        self.assertGreater(component_energy(level.riesz_x), component_energy(level.riesz_y) * 10.0)
        self.assertAlmostEqual(0.0, dominant_orientation(level), places=7)

    def test_vertical_ramp_has_stronger_y_component(self):
        image = [[float(y) for _ in range(8)] for y in range(6)]

        level = build_riesz_pyramid(image, levels=1)[0]

        self.assertGreater(component_energy(level.riesz_y), component_energy(level.riesz_x) * 10.0)
        self.assertAlmostEqual(math.pi / 2.0, dominant_orientation(level), places=7)

    def test_project_phase_uses_dominant_orientation(self):
        image = [[float(x + 1) for x in range(8)] for _ in range(6)]
        level = build_riesz_pyramid(image, levels=1)[0]

        phase_level = project_phase(level)

        self.assertAlmostEqual(0.0, phase_level.orientation_radians, places=7)
        self.assertEqual(image_size(level.image), image_size(phase_level.phase))
        self.assertGreater(component_energy(phase_level.amplitude), 0.0)

    def test_phase_delta_wraps_across_pi_boundary(self):
        reference = [[math.pi - 0.1]]
        current = [[-math.pi + 0.1]]

        delta = phase_delta(reference, current)

        self.assertAlmostEqual(0.2, delta[0][0], places=7)

    def test_amplify_phase_scales_wrapped_delta(self):
        reference = [[0.25]]
        current = [[0.35]]

        amplified = amplify_phase(reference, current, amplification=4.0)

        self.assertAlmostEqual(0.65, amplified[0][0], places=7)

    def test_smooth_phase_preserves_wrapped_boundary(self):
        phase = [
            [math.pi - 0.08, -math.pi + 0.08, math.pi - 0.08],
            [-math.pi + 0.08, math.pi - 0.08, -math.pi + 0.08],
            [math.pi - 0.08, -math.pi + 0.08, math.pi - 0.08],
        ]

        smoothed = smooth_phase3(phase)

        self.assertGreater(abs(smoothed[1][1]), math.pi - 0.2)

    def test_wrap_phase_normalizes_to_pi_range(self):
        self.assertAlmostEqual(math.pi, abs(wrap_phase(3.0 * math.pi)), places=7)

    def test_ragged_image_is_rejected(self):
        with self.assertRaises(ValueError):
            build_riesz_pyramid([[1.0, 2.0], [3.0]], levels=1)

    def test_phase_delta_requires_matching_sizes(self):
        with self.assertRaises(ValueError):
            phase_delta([[0.0, 0.1]], [[0.0]])


if __name__ == "__main__":
    unittest.main()
