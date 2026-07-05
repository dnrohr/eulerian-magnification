"""Run a deterministic synthetic Riesz-vs-linear-EVM comparison."""

from __future__ import annotations

from riesz_reference import compare_linear_and_phase


def synthetic_edge(width: int = 24, height: int = 16, edge_x: int = 10) -> list[list[float]]:
    return [
        [0.15 if x < edge_x else 0.85 for x in range(width)]
        for _ in range(height)
    ]


def main() -> None:
    reference = synthetic_edge(edge_x=10)
    shifted = synthetic_edge(edge_x=11)
    metrics = compare_linear_and_phase(reference, shifted, amplification=4.0)

    print("Synthetic one-pixel edge motion, amplification 4.0x")
    print(f"linear_mean_abs_delta={metrics.linear_mean_abs_delta:.6f}")
    print(f"phase_mean_abs_delta={metrics.phase_mean_abs_delta:.6f}")
    print(f"phase_to_linear_delta_ratio={metrics.phase_to_linear_delta_ratio:.6f}")
    print(f"linear_roughness={metrics.linear_roughness:.6f}")
    print(f"phase_roughness={metrics.phase_roughness:.6f}")


if __name__ == "__main__":
    main()
