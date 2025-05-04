const fs = require('fs');
const { execSync } = require('child_process');

// Try to install required packages if not already installed
try {
  execSync('which convert || (echo "ImageMagick not found. Please install it with: brew install imagemagick")');
} catch (error) {
  console.error('Error: ImageMagick is required but not installed.');
  console.error('Please install it with: brew install imagemagick');
  process.exit(1);
}

// Paths
const blackSvgPath = './space_data_logo.svg';
const whiteSvgPath = './space_data_logo_white.svg';
const blackPngPath = '../SpaceDataArchiveJava/src/main/resources/images/33fg_logo_black_transparent.png';
const whitePngPath = '../SpaceDataArchiveJava/src/main/resources/images/33fg_logo_white_transparent.png';

// Convert SVG to PNG
try {
  // Convert black logo
  execSync(`convert -background none -density 300 ${blackSvgPath} -resize 512x512 ${blackPngPath}`);
  console.log(`Successfully created black logo at ${blackPngPath}`);
  
  // Convert white logo
  execSync(`convert -background none -density 300 ${whiteSvgPath} -resize 512x512 ${whitePngPath}`);
  console.log(`Successfully created white logo at ${whitePngPath}`);
} catch (error) {
  console.error('Error converting SVG to PNG:', error.message);
  process.exit(1);
} 