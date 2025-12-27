# Light/Dark Theme Toggle - Disabled Feature

## Status
**Disabled** as of 2025-12-30. Dark mode is now the only theme.

## Reason for Disabling
- Maintenance overhead without proper vector assets for both themes
- No SVG logo that cleanly works on both light and dark backgrounds
- Simplifies UI and reduces sidebar clutter

## How to Re-enable

### 1. Menu.java Changes

In `init()` method, uncomment:
```java
add(lightDarkMode);
add(toolBarAccentColor);
```

In `setMenuFull()` method, uncomment:
```java
lightDarkMode.setMenuFull(menuFull);
toolBarAccentColor.setMenuFull(menuFull);
```

In `MenuLayout.layoutContainer()`, uncomment the layout calculations for:
- `ldgap`, `ldWidth`, `ldHeight`, `ldx`, `ldy`
- `lightDarkMode.setBounds(...)`
- `toolBarAccentColor.setBounds(...)`
- Restore `menuHeight` calculation to account for toggle space

### 2. Required Assets
Before re-enabling, ensure:
- Logo SVG works on both light and dark backgrounds
- Or create separate logo variants for each theme
- Test all icon visibility in both modes

### 3. Files Involved
- `src/main/java/org/whisperdog/sidemenu/Menu.java`
- `src/main/java/org/whisperdog/sidemenu/mode/LightDarkMode.java`
- `src/main/java/org/whisperdog/sidemenu/mode/ToolBarAccentColor.java`
- `src/main/resources/theme/FlatLightLaf.properties`
- `src/main/resources/theme/FlatDarkLaf.properties`

### 4. Testing Checklist
- [ ] Toggle switches themes correctly
- [ ] All icons visible in both themes
- [ ] Logo readable on both backgrounds
- [ ] Menu items remain readable
- [ ] Toast notifications work in both themes
