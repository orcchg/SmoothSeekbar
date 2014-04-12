/*
 *   Author: Maxim Alov <m.alov@samsung.com>
 */

package maxa.orcchg.smoothseekbar;

import maxa.orcchg.smoothseekbar.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Style;


public class SmoothSeekBar extends View {
  private static final String TAG = "seekbar";
  private static Integer height = 100;
  private OnSmoothSeekBarChangeListener m_listener;
  
  private final Paint m_progress_paint, m_rest_paint, m_cursor_paint, m_label_paint;
  
  private final float m_vertical_padding;
  private final float m_horizontal_padding;
  
  private final float m_cursor_rect_height;
  private final float m_cursor_rect_width;
  private final float m_box_rect_height;
  private final float m_box_rect_width;
  private final float m_gap;

  private float m_current_relative_position;  // cursor relative position
  private float m_max_relative_progress;  // max cursor relative position
  private float m_position;  // cursor absolute position
  private float m_max_position;  // max cursor absolute position
  private boolean m_is_dragging;
  
  private int m_total_labels;
  private float m_label_gap;  // width of gap between two tangent labels
  private int m_progress;
  
  
  /* Public API */
  // --------------------------------------------------------------------------
  public SmoothSeekBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    m_progress_paint = new Paint();
    m_progress_paint.setColor(getResources().getColor(R.color.seekbar_progress));
    m_rest_paint = new Paint();
    m_rest_paint.setColor(getResources().getColor(R.color.seekbar_rest));
    m_cursor_paint = new Paint();
    m_cursor_paint.setColor(getResources().getColor(R.color.seekbar_cursor));
    m_label_paint = new Paint();
    m_label_paint.setColor(getResources().getColor(R.color.seekbar_label));
    m_label_paint.setStyle(Style.FILL_AND_STROKE);
    
    TypedArray attributes_array = context.obtainStyledAttributes(attrs, R.styleable.SmoothSeekBar, 0, 0);
    m_vertical_padding = attributes_array.getDimension(R.styleable.SmoothSeekBar_vertical_padding, 0.0f);
    m_horizontal_padding = attributes_array.getDimension(R.styleable.SmoothSeekBar_horizontal_padding, 0.0f);
    
    m_cursor_rect_height = attributes_array.getDimension(R.styleable.SmoothSeekBar_cursor_height, 25.0f);
    m_cursor_rect_width = attributes_array.getDimension(R.styleable.SmoothSeekBar_cursor_width, 18.0f);
    m_box_rect_height = attributes_array.getDimension(R.styleable.SmoothSeekBar_box_height, 15.0f);
    m_box_rect_width = attributes_array.getDimension(R.styleable.SmoothSeekBar_box_width, 300.0f);
    m_gap = (m_cursor_rect_height - m_box_rect_height) * 0.5f;
    
    m_progress = attributes_array.getInteger(R.styleable.SmoothSeekBar_progress, 2);
    m_max_relative_progress = attributes_array.getDimension(R.styleable.SmoothSeekBar_max, 100.0f);
    m_total_labels = attributes_array.getInteger(R.styleable.SmoothSeekBar_total_labels, 10);
    attributes_array.recycle();
    
    m_max_position = m_horizontal_padding + m_max_relative_progress;
    m_is_dragging = false;
    
    m_label_gap = m_max_relative_progress / m_total_labels;
    m_current_relative_position = m_progress * m_label_gap;
    m_position = m_horizontal_padding + m_current_relative_position;
  }
  
  public void setMax(int max) {
    m_total_labels = max;
    m_label_gap = m_max_relative_progress / m_total_labels;
  }
  
  public void setProgress(int progress) {
    m_current_relative_position = progress * m_label_gap;
    m_position = m_horizontal_padding + m_current_relative_position;
    m_progress = progress;
    invalidate();
  }
  
  public void setOnSmoothSeekBarChangeListener(OnSmoothSeekBarChangeListener listener) {
    m_listener = listener;
  }
  
  public int getProgress() {
    return m_progress;
  }
  
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (event.getX() >= m_position &&
            event.getX() <= m_position + m_cursor_rect_width &&
            event.getY() >= m_vertical_padding &&
            event.getY() <= m_vertical_padding + m_cursor_rect_height) {
          // user touch draggable box - start dragging
          getParent().requestDisallowInterceptTouchEvent(true);
          m_is_dragging = true;
          m_listener.onStartTrackingTouch(this);
        }
        break;
      case MotionEvent.ACTION_MOVE:  
        handleMove(event);
        break;
      case MotionEvent.ACTION_UP:
        if (m_is_dragging) {
          m_is_dragging = false;
          int nearest_left_label = relativePositionToProgress();
          float nearest_left_label_distance = Math.abs(m_current_relative_position - nearest_left_label * m_label_gap);
          float nearest_right_label_distance = Math.abs((nearest_left_label + 1) * m_label_gap - m_current_relative_position);
          if (nearest_left_label_distance <= nearest_right_label_distance) {
            // flip to the left label
            flip(nearest_left_label);
          } else {
            // flip to the right label
            flip(nearest_left_label + 1);
          }
          m_listener.onStopTrackingTouch(this);
        }
        break;
    }
    return true;
  }
  
  @SuppressLint("DrawAllocation")
  @Override
  public void onDraw(Canvas canvas) {
    if (m_is_dragging) {
      m_position = m_horizontal_padding + m_current_relative_position;
    } else {
    }
    
    RectF progress_box = new RectF(
      m_horizontal_padding,
      m_vertical_padding + m_gap,
      m_horizontal_padding + m_current_relative_position,
      m_vertical_padding + m_gap + m_box_rect_height);
    canvas.drawRect(progress_box, m_progress_paint);
    
    RectF cursor_box = new RectF(
      m_horizontal_padding + m_current_relative_position,
      m_vertical_padding,
      m_horizontal_padding + m_current_relative_position + m_cursor_rect_width,
      m_vertical_padding + m_cursor_rect_height);
    canvas.drawRoundRect(cursor_box, 5, 5, m_cursor_paint);
    
    RectF rest_box = new RectF(
      m_horizontal_padding + m_current_relative_position + m_cursor_rect_width,
      m_vertical_padding + m_gap,
      m_horizontal_padding + m_cursor_rect_width + m_box_rect_width,
      m_vertical_padding + m_gap + m_box_rect_height);
    canvas.drawRect(rest_box, m_rest_paint);
    
    for (int i = 0; i <= m_total_labels; ++i) {
      canvas.drawCircle(m_horizontal_padding + m_label_gap * i, m_vertical_padding + m_gap + m_box_rect_height + m_gap * 0.5f, 2, m_label_paint);
    }
  }
  
  
  /* Private methods */
  // --------------------------------------------------------------------------
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
      height = 100;
    } else if (getLayoutParams().height == LayoutParams.MATCH_PARENT ||
               getLayoutParams().height == LayoutParams.FILL_PARENT) {
      height = MeasureSpec.getSize(heightMeasureSpec);
    } else {
      height = getLayoutParams().height;
    }
    setMeasuredDimension(widthMeasureSpec | MeasureSpec.EXACTLY, height | MeasureSpec.EXACTLY);
  }
  
  private void handleMove(MotionEvent event) {
    if (!m_is_dragging ||
        event.getX() > m_horizontal_padding + m_box_rect_width ||
        event.getX() > m_max_position) {
      return;
    }
    float actual_position = event.getX() - m_horizontal_padding;
    if (actual_position >= 0.0f) {
      m_current_relative_position = actual_position;
    } else {
      m_current_relative_position = 0.0f;
    }
    invalidate();
    m_progress = relativePositionToProgress();
    m_listener.onProgressChanged(this, m_progress, true);
  }
  
  private void flip(int label) {
    m_current_relative_position = m_label_gap * label;
    m_position = m_horizontal_padding + m_current_relative_position;
    invalidate();
    m_progress = relativePositionToProgress();
    m_listener.onProgressChanged(this, m_progress, true);
  }
  
  private int relativePositionToProgress() {
    return (int) Math.floor((m_current_relative_position) / m_label_gap);
  }
}
