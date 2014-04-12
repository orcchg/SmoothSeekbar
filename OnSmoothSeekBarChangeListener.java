package maxa.orcchg.smoothseekbar;

public interface OnSmoothSeekBarChangeListener {
  public void onProgressChanged(SmoothSeekBar seekBar, int progress, boolean fromUser);
  public void onStartTrackingTouch(SmoothSeekBar seekBar);
  public void onStopTrackingTouch(SmoothSeekBar seekBar);
}
