/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.concurrent.CompletableFuture;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class MonaLisaInCubeAugmentedNode extends AnchorNode implements AugmentedNode {

  private static final String TAG = "AugmentedVideoNode";

  // The augmented image represented by this node.
  private AugmentedImage image;
  private float lisa_scale = 0.0f;

  // Models of the 4 corners.  We use completable futures here to simplify
  // the error handling and asynchronous loading.  The loading is started with the
  // first construction of an instance, and then used when the image is set.
  private static CompletableFuture<ModelRenderable> monaLisa;

  public MonaLisaInCubeAugmentedNode(Context context) {
    // Upon construction, start loading the models for the corners of the frame.
    if (monaLisa == null) {
      monaLisa = ModelRenderable.builder()
              .setSource(context, Uri.parse("lisainthecube.sfb"))
              .build();
    }
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */
  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void setImage(AugmentedImage image) {
    this.image = image;

    // If any of the models are not loaded, then recurse when all are loaded.
    if (!monaLisa.isDone()) {
      CompletableFuture.allOf(monaLisa)
          .thenAccept((Void aVoid) -> setImage(image))
          .exceptionally(
              throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
              });
    }

    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    // Make the 4 corner nodes.
    Vector3 localPosition = new Vector3();

    // Make sure the longest edge fits inside the image.
    final float liza_max_size = 13.48f;
    final float max_image_edge = Math.max(image.getExtentX(), image.getExtentZ());
    lisa_scale = max_image_edge / liza_max_size;
    lisa_scale *= 8;

    // Mona lisa
    localPosition.set(0.0f * image.getExtentX(), -0.4f, 0 * image.getExtentZ());
    Node lisaNode = new Node();
    lisaNode.setParent(this);
    lisaNode.setLocalPosition(localPosition);
    lisaNode.setLocalScale(new Vector3(lisa_scale, lisa_scale, lisa_scale));
    Quaternion currentRotation = lisaNode.getLocalRotation();
    currentRotation.y = 180;
    lisaNode.setLocalRotation(currentRotation);
    lisaNode.setRenderable(monaLisa.getNow(null));

  }

  @Override
  public AugmentedImage getImage() {
    return image;
  }
}
