# Signal Source And Visualization Model

The app now separates three concepts that were previously folded into
`MagnificationMode` and `ViewMode`.

## Concepts

- `SignalSource`: the measured signal or analysis input.
- `RendererKind`: the renderer that produces the processed image.
- `VisualizationStyle`: how the output is presented to the user.

The model is implemented in `VisualizationModel`.

## Current Mapping

Recorded exports:

| Mode / View | Signal source | Renderer | Visualization |
| --- | --- | --- | --- |
| Any / Raw | Recorded ROI green bandpass | Raw passthrough | Raw |
| Any / Difference | Recorded ROI green bandpass | ROI signal diagnostic | ROI difference |
| Pulse / Amplified | Recorded ROI green bandpass | Recorded full-frame linear EVM | Full-frame amplified |
| Pulse / Split | Recorded ROI green bandpass | Recorded full-frame linear EVM | Split comparison |
| Motion modes / Amplified | Recorded ROI green bandpass | Recorded Riesz phase motion | Full-frame amplified |
| Motion modes / Split | Recorded ROI green bandpass | Recorded Riesz phase motion | Split comparison |

Live preview:

| Mode / View | Signal source | Renderer | Visualization |
| --- | --- | --- | --- |
| Pulse / Amplified or Split with healthy GL bridge | ROI green bandpass | Live GL full-frame color bridge | Full-frame amplified or split comparison |
| Pulse / Amplified without bridge | ROI green bandpass | Live ROI signal tint | ROI signal overlay |
| Breathing / any diagnostic view | ROI vertical translation | ROI signal diagnostic or tint | Diagnostic/overlay |
| Fast Motion / diagnostic views | ROI motion estimate | ROI signal diagnostic or tint | Diagnostic/overlay |

## Export Compatibility

Existing metadata fields remain:

- `mode`
- `viewMode`
- `lowCutHz`
- `highCutHz`
- `amplification`

New fields make the processing path explicit:

- `signalSource`
- `signalSourceLabel`
- `renderer`
- `rendererLabel`
- `visualizationStyle`
- `visualizationStyleLabel`

This keeps old exports readable while allowing new exports and evidence reports
to identify whether an output is ROI tint, full-frame linear EVM, Riesz phase
motion, or a diagnostic view.
