# Darwin Soldier Vue OreUI

The five existing screens use the local ApricityUI 1.2.4-hotfix runtime and its bundled Vue 3.5.34 / mcui-oreui 1.2.2 components. Forge 1.20.1 and NeoForge 1.21.1 share page resources and retain separate loader, configuration and gameplay implementations.

## Layout contract

Preserve the pre-migration page organization and density. The bounded screen has a 72 px header, one scrolling main region, and the existing fixed footer where applicable. Use the original browser viewport reference with zoom locked to 1; Minecraft GUI scale must not enlarge the document.

Growth retains its left-aligned title and subtitle, continuous five-value status strip, 38/62 attribute/ability split, seven ability selectors in one row, two-column ability details, and two deterministic combat-information columns. Attribute rows contain a label and minus/value/plus stepper followed by a slider. Remaining points and confirmation stay in the attribute panel footer. At 900 px the workspace stacks and abilities use four columns; at 560 px they use two.

Targeting retains a horizontal filter panel above the full-width searchable entity list. Configuration retains the category sidebar and field panel, with reset, cancel and save in the fixed footer. Aiming retains the full-width weapon panel with three tuning rows and footer actions. Adaptation records retain their paginated table and empty state.

## Components and state

Use actual McHeader, McPanel, McButton, McProgress, McSlider, McSwitch and McTextField Vue components. Reuse their Ore theme tokens and interaction states. Page CSS only supplies the required layout and dimensions. Small steppers and progress bars override component default widths within their own regions. Existing translated labels and values replace the slider's fixed English label. McTextField supports multiline lists through singleLine=false.

Java publishes structured darwin-state snapshots. Vue sends explicit JSON darwin-action and darwin-input events using the existing operation names. Server state, packets, staged allocations, numeric limits, pagination and configuration validation remain authoritative. Integer-valued JSON decimals are accepted as integers; fractional and overflowing integer values are rejected.

## Verification boundary

Native Minecraft/AUI screenshots, pointer and keyboard actions, window resizing and resource reloads establish UI evidence. Builds alone do not establish visual acceptance. Record automated/native checks separately from the user's explicit client-specific acceptance. Armor supply remains driven by held input and stops on release, focus loss or close.