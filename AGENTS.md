# Project instructions

## Kobweb UI conventions

- Prefer Kobweb and Silk components over raw HTML elements.
- Use `Box`, `Row`, `Column`, and `SimpleGrid` instead of raw `Div` elements.
- Prefer typed `Modifier` APIs over `styleModifier` / `property` calls.
- Prefer Font Awesome icons via Kobweb's `Fa*` components for UI icons instead
  of emoji, Unicode symbols, or custom-drawn icons when a suitable icon exists.
- Use raw HTML or CSS only when no suitable wrapper exists or semantic HTML
  requires it. Briefly explain non-obvious exceptions.
- Follow these conventions in new and modified code; avoid unrelated rewrites.
