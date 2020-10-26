package michael.wilson.datacubed3.Other;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/** AutoFitTextureView
 *
 * A custom TextureView which maintains a constant aspect ratio.
 */
public class AutoFitTextureView extends TextureView
{
    /** The ratio width and height of this texture view **/
    private int mRatioWidth = 0, mRatioHeight = 0;

    /** AutoFitTextureView()
     *
     * Constructor of the AutoFitTextureView class.
     *
     * @param context The context.
     */
    public AutoFitTextureView(Context context)
    {
        this(context, null);
    }
    public AutoFitTextureView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }
    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this TextureView
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height)
    {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Size cannot be negative.");
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    /** onMeasure()
     *
     * Called when this TextureView is measured.
     *
     * @param widthMeasureSpec The width measure spec
     * @param heightMeasureSpec The height measure spec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) setMeasuredDimension(width, height);
        else
        {
            if (width < height * mRatioWidth / mRatioHeight)   setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            else  setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
        }
    }

}