package ysklyarov.almatywear

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderChooserIntent
import android.support.wearable.complications.ProviderInfoRetriever
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import ysklyarov.almatywear.YurisWatchface

import java.util.concurrent.Executors

/**
 * The watch-side config activity for [ComplicationWatchFaceService], which allows for setting
 * the left and right complications of watch face.
 */
class ComplicationConfigActivity : Activity(), View.OnClickListener {

    private var mLeftComplicationId: Int = 0
    private var mRightComplicationId: Int = 0
    private var mLargeComplicationId: Int = 0

    // Selected complication id by user.
    private var mSelectedComplicationId: Int = 0

    // ComponentName used to identify a specific service that renders the watch face.
    private var mWatchFaceComponentName: ComponentName? = null

    // Required to retrieve complication data from watch face for preview.
    private var mProviderInfoRetriever: ProviderInfoRetriever? = null

    private var mLeftComplicationBackground: ImageView? = null
    private var mRightComplicationBackground: ImageView? = null
    private var mLargeComplicationBackground: ImageView? = null

    private var mLeftComplication: ImageButton? = null
    private var mRightComplication: ImageButton? = null
    private var mLargeComplication: ImageButton? = null

    private var mDefaultAddComplicationDrawable: Drawable? = null


    enum class ComplicationLocation {
        LARGE,
        LEFT,
        RIGHT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_config)

        mDefaultAddComplicationDrawable = getDrawable(R.drawable.add_complication)

        // TODO: Step 3, initialize 1
        mSelectedComplicationId = -1

        mLeftComplicationId = YurisWatchface.getComplicationId(ComplicationLocation.LEFT)
        mRightComplicationId = YurisWatchface.getComplicationId(ComplicationLocation.RIGHT)
        mLargeComplicationId = YurisWatchface.getComplicationId(ComplicationLocation.LARGE)

        mWatchFaceComponentName = ComponentName(applicationContext, YurisWatchface::class.java)

        // Sets up left complication preview.
        mLeftComplicationBackground = findViewById<View>(R.id.left_complication_background) as ImageView
        mLeftComplication = findViewById<View>(R.id.left_complication) as ImageButton
        mLeftComplication!!.setOnClickListener(this)

        // Sets default as "Add Complication" icon.
        mLeftComplication!!.setImageDrawable(mDefaultAddComplicationDrawable)
        mLeftComplicationBackground!!.visibility = View.INVISIBLE

        // Sets up right complication preview.
        mRightComplicationBackground = findViewById<View>(R.id.right_complication_background) as ImageView
        mRightComplication = findViewById<View>(R.id.right_complication) as ImageButton
        mRightComplication!!.setOnClickListener(this)

        // Sets default as "Add Complication" icon.
        mRightComplication!!.setImageDrawable(mDefaultAddComplicationDrawable)
        mRightComplicationBackground!!.visibility = View.INVISIBLE

        // Sets up large complication preview.
        mLargeComplicationBackground = findViewById<View>(R.id.large_complication_background) as ImageView
        mLargeComplication = findViewById<View>(R.id.large_complication) as ImageButton
        mLargeComplication!!.setOnClickListener(this)

        // Sets default as "Add Complication" icon.
        mLargeComplication!!.setImageDrawable(mDefaultAddComplicationDrawable)
        mLargeComplicationBackground!!.visibility = View.INVISIBLE

        mProviderInfoRetriever = ProviderInfoRetriever(applicationContext, Executors.newCachedThreadPool())
        mProviderInfoRetriever!!.init()

        retrieveInitialComplicationsData()
    }

    override fun onDestroy() {
        super.onDestroy()
        mProviderInfoRetriever!!.release()
    }

    fun retrieveInitialComplicationsData() {

        val complicationIds = YurisWatchface.getComplicationIds()

        mProviderInfoRetriever!!.retrieveProviderInfo(
                object : ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                    override fun onProviderInfoReceived(
                            watchFaceComplicationId: Int,
                            complicationProviderInfo: ComplicationProviderInfo?) {

//                        Log.d(TAG, "onProviderInfoReceived: " + complicationProviderInfo!!)

                        updateComplicationViews(watchFaceComplicationId, complicationProviderInfo)
                    }
                },
                mWatchFaceComponentName,
                *complicationIds)
    }

    override fun onClick(view: View) {
        if (view == mLeftComplication) {
//            Log.d(TAG, "Left Complication click()")
            launchComplicationHelperActivity(ComplicationLocation.LEFT)

        } else if (view == mRightComplication) {
//            Log.d(TAG, "Right Complication click()")
            launchComplicationHelperActivity(ComplicationLocation.RIGHT)
        } else if (view == mLargeComplication) {
//            Log.d(TAG, "Large Complication click()")
            launchComplicationHelperActivity(ComplicationLocation.LARGE)
        }
    }

    private fun launchComplicationHelperActivity(complicationLocation: ComplicationLocation) {

        mSelectedComplicationId = YurisWatchface.getComplicationId(complicationLocation)

        if (mSelectedComplicationId >= 0) {

            val supportedTypes = YurisWatchface.getSupportedComplicationTypes(
                    complicationLocation)

            startActivityForResult(
                    ComplicationHelperActivity.createProviderChooserHelperIntent(
                            applicationContext,
                            mWatchFaceComponentName,
                            mSelectedComplicationId,
                            *supportedTypes),
                    ComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE)

        } else {
            Log.d(TAG, "Complication not supported by watch face.")
        }
    }

    fun updateComplicationViews(watchFaceComplicationId: Int, complicationProviderInfo: ComplicationProviderInfo?) {
//        Log.d(TAG, "updateComplicationViews(): id: $watchFaceComplicationId")

        if (watchFaceComplicationId == mLeftComplicationId) {
            if (complicationProviderInfo != null) {
                mLeftComplication!!.setImageIcon(complicationProviderInfo.providerIcon)
                mLeftComplicationBackground!!.visibility = View.VISIBLE

            } else {
                mLeftComplication!!.setImageDrawable(mDefaultAddComplicationDrawable)
                mLeftComplicationBackground!!.visibility = View.INVISIBLE
            }

        } else if (watchFaceComplicationId == mRightComplicationId) {
            if (complicationProviderInfo != null) {
                mRightComplication!!.setImageIcon(complicationProviderInfo.providerIcon)
                mRightComplicationBackground!!.visibility = View.VISIBLE

            } else {
                mRightComplication!!.setImageDrawable(mDefaultAddComplicationDrawable)
                mRightComplicationBackground!!.visibility = View.INVISIBLE
            }
        } else if (watchFaceComplicationId == mLargeComplicationId) {
            if (complicationProviderInfo != null) {
                mLargeComplication!!.setImageIcon(complicationProviderInfo.providerIcon)
                mRightComplicationBackground!!.visibility = View.VISIBLE

            } else {
                mLargeComplication!!.setImageDrawable(mDefaultAddComplicationDrawable)
                mRightComplicationBackground!!.visibility = View.INVISIBLE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            // Retrieves information for selected Complication provider.
            val complicationProviderInfo = data.getParcelableExtra<ComplicationProviderInfo>(ProviderChooserIntent.EXTRA_PROVIDER_INFO)
//            Log.d(TAG, "Provider: $complicationProviderInfo")

            if (mSelectedComplicationId >= 0) {
                updateComplicationViews(mSelectedComplicationId, complicationProviderInfo)
            }
        }
    }

    companion object {

        private val TAG = "ConfigActivity"

        internal val COMPLICATION_CONFIG_REQUEST_CODE = 1001
    }
}
