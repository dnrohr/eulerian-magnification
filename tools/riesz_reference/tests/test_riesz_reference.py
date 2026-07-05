import sys
from pathlib import Path
import unittest


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from riesz_reference import (  # noqa: E402
    build_gaussian_pyramid,
    build_riesz_pyramid,
    component_energy,
    image_size,
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

    def test_vertical_ramp_has_stronger_y_component(self):
        image = [[float(y) for _ in range(8)] for y in range(6)]

        level = build_riesz_pyramid(image, levels=1)[0]

        self.assertGreater(component_energy(level.riesz_y), component_energy(level.riesz_x) * 10.0)

    def test_ragged_image_is_rejected(self):
        with self.assertRaises(ValueError):
            build_riesz_pyramid([[1.0, 2.0], [3.0]], levels=1)


if __name__ == "__main__":
    unittest.main()
