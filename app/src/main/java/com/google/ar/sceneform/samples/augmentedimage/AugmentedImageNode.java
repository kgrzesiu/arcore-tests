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
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.util.concurrent.CompletableFuture;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode implements AugmentedNode {

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;
  @Nullable
  private ModelRenderable videoRenderable;
  private MediaPlayer mediaPlayer;
  private ExternalTexture texture;

  // The color to filter out of the video.
  private static final Color CHROMA_KEY_COLOR = new Color(0.1843f, 1.0f, 0.098f);

  // Controls the height of the video in world space.
  private static final float VIDEO_HEIGHT_METERS = 0.85f;

  public AugmentedImageNode(Context context) {

    // Create an ExternalTexture for displaying the contents of the video.
    texture = new ExternalTexture();

    // Create an Android MediaPlayer to capture the video on the external texture's surface.
    mediaPlayer = MediaPlayer.create(context, R.raw.lion_chroma);
    mediaPlayer.setSurface(texture.getSurface());
    mediaPlayer.setLooping(true);

    ModelRenderable.builder()
            .setSource(context, R.raw.chroma_key_video)
            .build()
            .thenAccept(
                    renderable -> {
                      videoRenderable = renderable;
                      renderable.getMaterial().setExternalTexture("videoTexture", texture);
                      renderable.getMaterial().setFloat4("keyColor", CHROMA_KEY_COLOR);
                    })
            .exceptionally(
                    throwable -> {
                      Toast toast =
                              Toast.makeText(context, "Unable to load video renderable", Toast.LENGTH_LONG);
                      toast.setGravity(Gravity.CENTER, 0, 0);
                      toast.show();
                      return null;
                    });
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void setImage(AugmentedImage image) {
    this.image = image;

    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    Node videoNode = new Node();
    videoNode.setParent(this);

    // Set the scale of the node so that the aspect ratio of the video is correct.
    float videoWidth = mediaPlayer.getVideoWidth();
    float videoHeight = mediaPlayer.getVideoHeight();
    float scaleFactor = 0.5f;

    videoNode.setLocalScale(
            new Vector3(
                    VIDEO_HEIGHT_METERS * scaleFactor * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS * scaleFactor, 1.0f));
    //position
    Vector3 localPosition = new Vector3();
    localPosition.set(0.0f * image.getExtentX(), -0.08f, 0 * image.getExtentZ());
    videoNode.setLocalPosition(localPosition);

    //fix lion rotation
    Quaternion currentRotation = videoNode.getLocalRotation();
//    currentRotation.x = 0;
//    currentRotation.y = -90;
    currentRotation.z = 90;
    currentRotation.w = 180;
    videoNode.setLocalRotation(currentRotation);

    // Start playing the video when the first node is placed.
    if (!mediaPlayer.isPlaying()) {
      mediaPlayer.start();

      // Wait to set the renderable until the first frame of the  video becomes available.
      // This prevents the renderable from briefly appearing as a black quad before the video
      // plays.
      texture
              .getSurfaceTexture()
              .setOnFrameAvailableListener(
                      (SurfaceTexture surfaceTexture) -> {
                        videoNode.setRenderable(videoRenderable);
                        texture.getSurfaceTexture().setOnFrameAvailableListener(null);
                      });
    } else {
      videoNode.setRenderable(videoRenderable);
    }

  }

  public AugmentedImage getImage() {
    return image;
  }
}
