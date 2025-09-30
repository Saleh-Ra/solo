# Menu Item Images

This folder contains images for menu items. Add the following images to make the menu display properly:

## Required Images:
- pizza.jpg
- hamburger.jpg
- vegan_hamburger.jpg
- pasta.jpg
- salad.jpg
- coca_cola.jpg
- ice_tea.jpg
- garlic_bread.jpg
- chocolate_cake.jpg
- orange_juice.jpg
- tiramisu.jpg
- main_course.jpg
- appetizer.jpg
- beverage.jpg
- dessert.jpg
- default_meal.jpg

## Image Specifications:
- Format: JPG or PNG
- Recommended size: 400x240 pixels (2:1 aspect ratio)
- The images will be displayed as 200x120 pixel buttons

## How to Add Images:
1. Download or create images for each menu item
2. Save them in this folder with the exact names listed above
3. If an image is missing, the system will fall back to a text "Add to Cart" button

## Fallback Behavior:
If an image file is not found, the system will:
1. Try to load the default_meal.jpg image
2. If that also fails, display a text button instead
3. Log an error message for debugging
