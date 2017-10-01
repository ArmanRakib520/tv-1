/*
 * Copyright 2016 Martin Kamp Jensen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mkjensen.tv.player;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.media.tv.companionlibrary.TvPlayer;

import android.content.Context;
import android.media.PlaybackParams;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import com.github.mkjensen.tv.TvApplication;

import javax.inject.Inject;

public class Player implements TvPlayer, EventListener {

  private static final String TAG = "Player";

  /**
   * The maximum bitrate in bits per second that should be assumed when a bandwidth estimate is
   * unavailable.
   */
  private static final int MAX_INITIAL_BITRATE = 10 * 1024 * 1024;

  @Inject
  BandwidthMeter bandwidthMeter;

  @Inject
  DataSource.Factory dataSourceFactory;

  private final TvInputService.Session session;

  private final SimpleExoPlayer simpleExoPlayer;

  public Player(@NonNull Context context, @NonNull TvInputService.Session session) {

    ((TvApplication) context.getApplicationContext()).getBackendComponent().inject(this);

    this.session = session;
    this.simpleExoPlayer = createSimpleExoPlayer(context);
  }

  @NonNull
  private SimpleExoPlayer createSimpleExoPlayer(@NonNull Context context) {

    AdaptiveTrackSelection.Factory atsFactory = new AdaptiveTrackSelection.Factory(
        bandwidthMeter,
        MAX_INITIAL_BITRATE,
        AdaptiveTrackSelection.DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
        AdaptiveTrackSelection.DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
        AdaptiveTrackSelection.DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
        AdaptiveTrackSelection.DEFAULT_BANDWIDTH_FRACTION
    );

    SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(
        context,
        new DefaultTrackSelector(atsFactory),
        new DefaultLoadControl()
    );

    player.addListener(this);

    return player;
  }

  public void play(@NonNull String url) {

    HlsMediaSource hlsMediaSource = new HlsMediaSource(
        Uri.parse(url),
        dataSourceFactory,
        null,
        null);

    simpleExoPlayer.prepare(hlsMediaSource);
    simpleExoPlayer.setPlayWhenReady(true);
  }

  public void release() {

    simpleExoPlayer.release();
  }

  @Override
  public void seekTo(long position) {

    simpleExoPlayer.seekTo(position);
  }

  @Override
  public void setPlaybackParams(@Nullable PlaybackParams params) {

    simpleExoPlayer.setPlaybackParams(params);
  }

  @Override
  public long getCurrentPosition() {

    return simpleExoPlayer.getCurrentPosition();
  }

  @Override
  public long getDuration() {

    return simpleExoPlayer.getDuration();
  }

  @Override
  public void setSurface(@Nullable Surface surface) {

    simpleExoPlayer.setVideoSurface(surface);
  }

  @Override
  public void setVolume(float volume) {

    simpleExoPlayer.setVolume(volume);
  }

  @Override
  public void pause() {

    simpleExoPlayer.setPlayWhenReady(false);

  }

  @Override
  public void play() {

    simpleExoPlayer.setPlayWhenReady(true);
  }

  @Override
  public void registerCallback(@NonNull Callback callback) {

    Log.d(TAG, "registerCallback " + callback);
    // Do nothing.
  }

  @Override
  public void unregisterCallback(@NonNull Callback callback) {

    Log.d(TAG, "unregisterCallback " + callback);
    // Do nothing.
  }

  @Override
  public void onLoadingChanged(boolean isLoading) {

    Log.d(TAG, "onLoadingChanged " + isLoading);
    // Do nothing.
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    Log.d(TAG, "onPlayerStateChanged " + playWhenReady + ", " + playbackState);

    if (!playWhenReady) {
      return;
    }

    switch (playbackState) {

      case ExoPlayer.STATE_BUFFERING:
        session.notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
        break;

      case ExoPlayer.STATE_READY:
        session.notifyVideoAvailable();
        break;
    }
  }

  @Override
  public void onRepeatModeChanged(int repeatMode) {

    Log.d(TAG, "onRepeatModeChanged " + repeatMode);
    // Do nothing.
  }

  @Override
  public void onTimelineChanged(@Nullable Timeline timeline, @Nullable Object manifest) {

    Log.d(TAG, "onTimelineChanged " + timeline + ", " + manifest);
    // Do nothing.
  }

  @Override
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    Log.d(TAG, "onTracksChanged " + trackGroups + ", " + trackSelections);
    // Do nothing.
  }

  @Override
  public void onPlayerError(@NonNull ExoPlaybackException error) {

    Log.e(TAG, "An error occurred during playback", error);
    session.notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
  }

  @Override
  public void onPositionDiscontinuity() {

    Log.d(TAG, "onPositionDiscontinuity");
    // Do nothing.
  }

  @Override
  public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    Log.d(TAG, "onPlaybackParametersChanged " + playbackParameters);
    // Do nothing.
  }
}
