package com.google.ar.sceneform.samples.augmentedimage;

import com.google.ar.core.AugmentedImage;

interface AugmentedNode {
    void setImage(AugmentedImage image);
    AugmentedImage getImage();
}
