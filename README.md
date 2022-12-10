# Android Lane Detection
This is the repository for the project Lane Detection from the elective "Digital Image Processing" at DHBW Stuttgart.
## Lane Detection
To detect the lane several steps are taken:
1. Perspective Transformation
2. Convert the image to HSV and apply two color filter (one for the yellow line and one for the white)
3. Apply a morhological transformation and a threshold value 
4. Apply the morphological transformation with the gradient to filter the edges or outlines of the lanes. 
5. With the help of the Hough Line Transform the lanes are detected. 
6. The lanes are drawn on a new image and transformed back and combined with the camera image
## License
Licensed under [MIT](LICENSE.md).