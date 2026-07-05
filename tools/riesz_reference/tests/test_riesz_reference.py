import sys
from pathlib import Path
import json
import math
import tempfile
import unittest


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from riesz_reference import (  # noqa: E402
    build_gaussian_pyramid,
    build_riesz_pyramid,
    component_energy,
    amplify_phase,
    compare_linear_and_phase,
    dominant_orientation,
    image_roughness,
    image_size,
    linear_evm_frame,
    mean_abs_difference,
    phase_delta,
    phase_magnified_frame,
    project_phase,
    reconstruct_from_phase,
    smooth_phase3,
    wrap_phase,
)
from validate_sample_sequences import (  # noqa: E402
    SampleSequence,
    flat_frame,
    validate_known_sequences,
    validate_sample_sequence,
    vertical_edge,
)
from validate_decoded_sample import validate_decoded_sample  # noqa: E402


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

    def test_reconstruct_from_phase_preserves_flat_positive_image(self):
        image = [[0.5 for _ in range(4)] for _ in range(3)]
        phase_level = project_phase(build_riesz_pyramid(image, levels=1)[0])

        reconstructed = reconstruct_from_phase(phase_level, phase_level.phase)

        self.assertAlmostEqual(0.0, mean_abs_difference(image, reconstructed), places=7)

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

    def test_linear_evm_frame_amplifies_intensity_delta(self):
        reference = [[0.2, 0.2]]
        current = [[0.2, 0.3]]

        amplified = linear_evm_frame(reference, current, amplification=3.0)

        self.assertAlmostEqual(0.2, amplified[0][0], places=7)
        self.assertAlmostEqual(0.5, amplified[0][1], places=7)

    def test_phase_magnified_frame_changes_translated_edge(self):
        reference = synthetic_edge(edge_x=4)
        shifted = synthetic_edge(edge_x=5)

        phase = phase_magnified_frame(reference, shifted, amplification=4.0)

        self.assertGreater(mean_abs_difference(reference, phase), 0.0)
        self.assertGreater(image_roughness(phase), 0.0)

    def test_compare_linear_and_phase_returns_deterministic_metrics(self):
        reference = synthetic_edge(edge_x=4)
        shifted = synthetic_edge(edge_x=5)

        metrics = compare_linear_and_phase(reference, shifted, amplification=4.0)

        self.assertGreater(metrics.linear_mean_abs_delta, 0.0)
        self.assertGreater(metrics.phase_mean_abs_delta, 0.0)
        self.assertGreater(metrics.phase_to_linear_delta_ratio, 0.0)
        self.assertGreater(metrics.linear_roughness, 0.0)
        self.assertGreater(metrics.phase_roughness, 0.0)

    def test_ragged_image_is_rejected(self):
        with self.assertRaises(ValueError):
            build_riesz_pyramid([[1.0, 2.0], [3.0]], levels=1)

    def test_phase_delta_requires_matching_sizes(self):
        with self.assertRaises(ValueError):
            phase_delta([[0.0, 0.1]], [[0.0]])

    def test_known_sample_sequence_validation_passes(self):
        results = validate_known_sequences()

        self.assertEqual(3, len(results))
        self.assertTrue(all(result.passed for result in results), [result.summary() for result in results])

    def test_stationary_sequence_validation_rejects_false_motion(self):
        result = validate_sample_sequence(
            SampleSequence(
                name="stationary",
                frames=[flat_frame() for _ in range(3)],
                expected_orientation_radians=None,
                expects_motion=True,
            )
        )

        self.assertFalse(result.passed)

    def test_motion_sequence_validation_rejects_wrong_orientation(self):
        result = validate_sample_sequence(
            SampleSequence(
                name="wrong orientation",
                frames=[vertical_edge(edge_x=x) for x in (10, 11, 12)],
                expected_orientation_radians=math.pi / 2.0,
                expects_motion=True,
            )
        )

        self.assertFalse(result.passed)

    def test_decoded_sample_validation_accepts_frame_sequence_json(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "sample.json"
            path.write_text(
                json.dumps(
                    {
                        "source": "synthetic-motion.mp4",
                        "frames": [vertical_edge(edge_x=x) for x in range(10, 31)],
                    }
                )
            )

            result = validate_decoded_sample(path)

        self.assertTrue(result.passed, result.summary())
        self.assertEqual("synthetic-motion.mp4", result.source)

    def test_decoded_sample_validation_rejects_static_json(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "sample.json"
            path.write_text(
                json.dumps(
                    {
                        "source": "static.mp4",
                        "frames": [flat_frame() for _ in range(20)],
                    }
                )
            )

            result = validate_decoded_sample(path)

        self.assertFalse(result.passed)


def synthetic_edge(width=12, height=8, edge_x=4):
    return [
        [0.15 if x < edge_x else 0.85 for x in range(width)]
        for _ in range(height)
    ]


if __name__ == "__main__":
    unittest.main()
