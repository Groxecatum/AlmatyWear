//package ysklyarov.almatywear.config
//
//import android.app.Activity
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.content.SharedPreferences
//import android.graphics.Color
//import android.graphics.PorterDuff
//import android.graphics.PorterDuffColorFilter
//import android.graphics.drawable.Drawable
//import android.support.v7.widget.RecyclerView
//import android.support.wearable.complications.ComplicationHelperActivity
//import android.support.wearable.complications.ComplicationProviderInfo
//import android.support.wearable.complications.ProviderInfoRetriever
//import android.support.wearable.complications.ProviderInfoRetriever.OnProviderInfoReceivedCallback
//import android.util.Log
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.View
//import android.view.View.OnClickListener
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.ImageButton
//import android.widget.ImageView
//import android.widget.Switch
//import android.widget.Toast
//import ysklyarov.almatywear.YurisWatchface
//import ysklyarov.almatywear.model.ConfigData
//
//import java.util.ArrayList
//import java.util.concurrent.Executors
//
///**
// * Displays different layouts for configuring watch face's complications and appearance settings
// * (highlight color [second arm], background color, unread notifications, etc.).
// *
// *
// * All appearance settings are saved via [SharedPreferences].
// *
// *
// * Layouts provided by this adapter are split into 5 main view types.
// *
// *
// * A watch face preview including complications. Allows user to tap on the complications to
// * change the complication data and see a live preview of the watch face.
// *
// *
// * Simple arrow to indicate there are more options below the fold.
// *
// *
// * Color configuration options for both highlight (seconds hand) and background color.
// *
// *
// * Toggle for unread notifications.
// *
// *
// * Background image complication configuration for changing background image of watch face.
// */
//class ConfigAdapter(
//        private val mContext: Context,
//        watchFaceServiceClass: Class<*>,
//        private val mSettingsDataSet: ArrayList<ConfigItemType>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//    // ComponentName associated with watch face service (service that renders watch face). Used
//    // to retrieve complication information.
//    private val mWatchFaceComponentName: ComponentName
//
////    internal var mSharedPref: SharedPreferences
//
//    // Selected complication id by user.
//    private var mSelectedComplicationId: Int = 0
//
//    private val mBackgroundComplicationId: Int
//    private val mLeftComplicationId: Int
//    private val mRightComplicationId: Int
//
//    // Required to retrieve complication data from watch face for preview.
//    private val mProviderInfoRetriever: ProviderInfoRetriever
//
//    // Maintains reference view holder to dynamically update watch face preview. Used instead of
//    // notifyItemChanged(int position) to avoid flicker and re-inflating the view.
//    private var mPreviewAndComplicationsViewHolder: PreviewAndComplicationsViewHolder? = null
//
//    /**
//     * Used by associated watch face ([AnalogComplicationWatchFaceService]) to let this
//     * adapter know which complication locations are supported, their ids, and supported
//     * complication data types.
//     */
//    enum class ComplicationLocation {
//        BACKGROUND,
//        LEFT,
//        RIGHT,
//        TOP,
//        BOTTOM
//    }
//
//    init {
//        mWatchFaceComponentName = ComponentName(mContext, watchFaceServiceClass)
//
//        // Default value is invalid (only changed when user taps to change complication).
//        mSelectedComplicationId = -1
//
//        mBackgroundComplicationId = YurisWatchface.getComplicationId(
//                ComplicationLocation.BACKGROUND)
//
//        mLeftComplicationId = YurisWatchface.getComplicationId(ComplicationLocation.LEFT)
//        mRightComplicationId = YurisWatchface.getComplicationId(ComplicationLocation.RIGHT)
//
////        mSharedPref = mContext.getSharedPreferences(
////                mContext.getString(R.string.analog_complication_preference_file_key),
////                Context.MODE_PRIVATE)
//
//        // Initialization of code to retrieve active complication data for the watch face.
//        mProviderInfoRetriever = ProviderInfoRetriever(mContext, Executors.newCachedThreadPool())
//        mProviderInfoRetriever.init()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        Log.d(TAG, "onCreateViewHolder(): viewType: $viewType")
//
//        var viewHolder: RecyclerView.ViewHolder? = null
//
//        when (viewType) {
//            TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG -> {
//                // Need direct reference to watch face preview view holder to update watch face
//                // preview based on selections from the user.
//                mPreviewAndComplicationsViewHolder = PreviewAndComplicationsViewHolder(
//                        LayoutInflater.from(parent.context)
//                                .inflate(
//                                        R.layout.config_list_preview_and_complications_item,
//                                        parent,
//                                        false))
//                viewHolder = mPreviewAndComplicationsViewHolder
//            }
//        }
//
//        return viewHolder
//    }
//
//    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
//        Log.d(TAG, "Element $position set.")
//
//        // Pulls all data required for creating the UX for the specific setting option.
//        val configItemType = mSettingsDataSet[position]
//
//        when (viewHolder.itemViewType) {
//            TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG -> {
//                val previewAndComplicationsViewHolder = viewHolder as PreviewAndComplicationsViewHolder
//
//                val previewAndComplicationsConfigItem = configItemType as ConfigData.PreviewAndComplicationsConfigItem
//
//                val defaultComplicationResourceId = previewAndComplicationsConfigItem.getDefaultComplicationResourceId()
//                previewAndComplicationsViewHolder.setDefaultComplicationDrawable(
//                        defaultComplicationResourceId)
//
//                previewAndComplicationsViewHolder.initializesColorsAndComplications()
//            }
//        }
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        val configItemType = mSettingsDataSet[position]
//        return configItemType.getConfigType()
//    }
//
//    override fun getItemCount(): Int {
//        return mSettingsDataSet.size
//    }
//
//    /** Updates the selected complication id saved earlier with the new information.  */
//    fun updateSelectedComplication(complicationProviderInfo: ComplicationProviderInfo) {
//
//        Log.d(TAG, "updateSelectedComplication: " + mPreviewAndComplicationsViewHolder!!)
//
//        // Checks if view is inflated and complication id is valid.
//        if (mPreviewAndComplicationsViewHolder != null && mSelectedComplicationId >= 0) {
//            mPreviewAndComplicationsViewHolder!!.updateComplicationViews(
//                    mSelectedComplicationId, complicationProviderInfo)
//        }
//    }
//
//    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
//        super.onDetachedFromRecyclerView(recyclerView)
//        // Required to release retriever for active complication data on detach.
//        mProviderInfoRetriever.release()
//    }
//
//    fun updatePreviewColors() {
//        Log.d(TAG, "updatePreviewColors(): " + mPreviewAndComplicationsViewHolder!!)
//
//        if (mPreviewAndComplicationsViewHolder != null) {
//            mPreviewAndComplicationsViewHolder!!.updateWatchFaceColors()
//        }
//    }
//
//    /**
//     * Displays watch face preview along with complication locations. Allows user to tap on the
//     * complication they want to change and preview updates dynamically.
//     */
//    inner class PreviewAndComplicationsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
//
//        private val mWatchFaceArmsAndTicksView: View
//        private val mWatchFaceHighlightPreviewView: View
//        private val mWatchFaceBackgroundPreviewImageView: ImageView
//
//        private val mLeftComplicationBackground: ImageView
//        private val mRightComplicationBackground: ImageView
//
//        private val mLeftComplication: ImageButton
//        private val mRightComplication: ImageButton
//
//        private var mDefaultComplicationDrawable: Drawable? = null
//
//        private var mBackgroundComplicationEnabled: Boolean = false
//
//        init {
//
//            mWatchFaceBackgroundPreviewImageView = view.findViewById(R.id.watch_face_background) as ImageView
//            mWatchFaceArmsAndTicksView = view.findViewById(R.id.watch_face_arms_and_ticks)
//
//            // In our case, just the second arm.
//            mWatchFaceHighlightPreviewView = view.findViewById(R.id.watch_face_highlight)
//
//            // Sets up left complication preview.
//            mLeftComplicationBackground = view.findViewById(R.id.left_complication_background) as ImageView
//            mLeftComplication = view.findViewById(R.id.left_complication) as ImageButton
//            mLeftComplication.setOnClickListener(this)
//
//            // Sets up right complication preview.
//            mRightComplicationBackground = view.findViewById(R.id.right_complication_background) as ImageView
//            mRightComplication = view.findViewById(R.id.right_complication) as ImageButton
//            mRightComplication.setOnClickListener(this)
//        }
//
//        override fun onClick(view: View) {
//            if (view == mLeftComplication) {
//                Log.d(TAG, "Left Complication click()")
//
//                val currentActivity = view.context as Activity
//                launchComplicationHelperActivity(currentActivity, ComplicationLocation.LEFT)
//
//            } else if (view == mRightComplication) {
//                Log.d(TAG, "Right Complication click()")
//
//                val currentActivity = view.context as Activity
//                launchComplicationHelperActivity(currentActivity, ComplicationLocation.RIGHT)
//            }
//        }
//
//        fun updateWatchFaceColors() {
//
//            // Only update background colors for preview if background complications are disabled.
//            if (!mBackgroundComplicationEnabled) {
//                val backgroundColorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP)
//
//                mWatchFaceBackgroundPreviewImageView
//                        .background.colorFilter = backgroundColorFilter
//
//            } else {
//                // Inform user that they need to disable background image for color to work.
//                val text = "Selected image overrides background color."
//                val duration = Toast.LENGTH_SHORT
//                val toast = Toast.makeText(mContext, text, duration)
//                toast.setGravity(Gravity.CENTER, 0, 0)
//                toast.show()
//            }
//
//            val highlightColorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
//
//            mWatchFaceHighlightPreviewView.background.colorFilter = highlightColorFilter
//        }
//
//        // Verifies the watch face supports the complication location, then launches the helper
//        // class, so user can choose their complication data provider.
//        private fun launchComplicationHelperActivity(
//                currentActivity: Activity, complicationLocation: ComplicationLocation) {
//
//            mSelectedComplicationId = YurisWatchface.getComplicationId(complicationLocation)
//
//            mBackgroundComplicationEnabled = false
//
//            if (mSelectedComplicationId >= 0) {
//
//                val supportedTypes = YurisWatchface.getSupportedComplicationTypes(
//                        complicationLocation)
//
//                val watchFace = ComponentName(currentActivity, YurisWatchface::class.java)
//
//                currentActivity.startActivityForResult(
//                        ComplicationHelperActivity.createProviderChooserHelperIntent(
//                                currentActivity,
//                                watchFace,
//                                mSelectedComplicationId,
//                                *supportedTypes),
//                        ComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE)
//
//            } else {
//                Log.d(TAG, "Complication not supported by watch face.")
//            }
//        }
//
//        fun setDefaultComplicationDrawable(resourceId: Int) {
//            val context = mWatchFaceArmsAndTicksView.context
//            mDefaultComplicationDrawable = context.getDrawable(resourceId)
//
//            mLeftComplication.setImageDrawable(mDefaultComplicationDrawable)
//            mLeftComplicationBackground.visibility = View.INVISIBLE
//
//            mRightComplication.setImageDrawable(mDefaultComplicationDrawable)
//            mRightComplicationBackground.visibility = View.INVISIBLE
//        }
//
//        fun updateComplicationViews(
//                watchFaceComplicationId: Int, complicationProviderInfo: ComplicationProviderInfo?) {
//            Log.d(TAG, "updateComplicationViews(): id: $watchFaceComplicationId")
//            Log.d(TAG, "\tinfo: " + complicationProviderInfo!!)
//
//            if (watchFaceComplicationId == mBackgroundComplicationId) {
//                if (complicationProviderInfo != null) {
//                    mBackgroundComplicationEnabled = true
//
//                    // Since we can't get the background complication image outside of the
//                    // watch face, we set the icon for that provider instead with a gray background.
//                    val backgroundColorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
//
//                    mWatchFaceBackgroundPreviewImageView
//                            .background.colorFilter = backgroundColorFilter
//                    mWatchFaceBackgroundPreviewImageView.setImageIcon(
//                            complicationProviderInfo.providerIcon)
//
//                } else {
//                    mBackgroundComplicationEnabled = false
//
//                    // Clears icon for background if it was present before.
//                    mWatchFaceBackgroundPreviewImageView.setImageResource(
//                            android.R.color.transparent)
//
//                    val backgroundColorFilter = PorterDuffColorFilter(
//                            Color.BLACK, PorterDuff.Mode.SRC_ATOP)
//
//                    mWatchFaceBackgroundPreviewImageView
//                            .background.colorFilter = backgroundColorFilter
//                }
//
//            } else if (watchFaceComplicationId == mLeftComplicationId) {
//                updateComplicationView(complicationProviderInfo, mLeftComplication,
//                        mLeftComplicationBackground)
//
//            } else if (watchFaceComplicationId == mRightComplicationId) {
//                updateComplicationView(complicationProviderInfo, mRightComplication,
//                        mRightComplicationBackground)
//            }
//        }
//
//        private fun updateComplicationView(complicationProviderInfo: ComplicationProviderInfo?,
//                                           button: ImageButton, background: ImageView) {
//            if (complicationProviderInfo != null) {
//                button.setImageIcon(complicationProviderInfo.providerIcon)
//                button.contentDescription = mContext.getString(R.string.edit_complication,
//                        complicationProviderInfo.appName + " " +
//                                complicationProviderInfo.providerName)
//                background.visibility = View.VISIBLE
//            } else {
//                button.setImageDrawable(mDefaultComplicationDrawable)
//                button.contentDescription = mContext.getString(R.string.add_complication)
//                background.visibility = View.INVISIBLE
//            }
//        }
//
//        fun initializesColorsAndComplications() {
//
//            val highlightColorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
//
//            mWatchFaceHighlightPreviewView.background.colorFilter = highlightColorFilter
//
//            // Initializes background color to gray (updates to color or complication icon based
//            // on whether the background complication is live or not.
//            val backgroundColorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP)
//
//            mWatchFaceBackgroundPreviewImageView
//                    .background.colorFilter = backgroundColorFilter
//
//            val complicationIds = YurisWatchface.getComplicationIds()
//
//            mProviderInfoRetriever.retrieveProviderInfo(
//                    object : ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
//                        override fun onProviderInfoReceived(
//                                watchFaceComplicationId: Int,
//                                complicationProviderInfo: ComplicationProviderInfo?) {
//
//                            Log.d(TAG, "onProviderInfoReceived: " + complicationProviderInfo!!)
//
//                            updateComplicationViews(
//                                    watchFaceComplicationId, complicationProviderInfo)
//                        }
//                    },
//                    mWatchFaceComponentName,
//                    *complicationIds)
//        }
//    }
//
//    /** Displays icon to indicate there are more options below the fold.  */
//    inner class MoreOptionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//
//        private val mMoreOptionsImageView: ImageView
//
//        init {
//            mMoreOptionsImageView = view.findViewById(R.id.more_options_image_view) as ImageView
//        }
//
//        fun setIcon(resourceId: Int) {
//            val context = mMoreOptionsImageView.context
//            mMoreOptionsImageView.setImageDrawable(context.getDrawable(resourceId))
//        }
//    }
//
//    /**
//     * Displays color options for the an item on the watch face. These could include marker color,
//     * background color, etc.
//     */
//    inner class ColorPickerViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
//
//        private val mAppearanceButton: Button
//
//        //private var mSharedPrefResourceString: String? = null
//
//        //private var mLaunchActivityToSelectColor: Class<ColorSelectionActivity>? = null
//
//        init {
//
//            mAppearanceButton = view.findViewById(R.id.color_picker_button) as Button
//            view.setOnClickListener(this)
//        }
//
//        fun setName(name: String) {
//            mAppearanceButton.text = name
//        }
//
//        fun setIcon(resourceId: Int) {
//            val context = mAppearanceButton.context
//            mAppearanceButton.setCompoundDrawablesWithIntrinsicBounds(
//                    context.getDrawable(resourceId), null, null, null)
//        }
//
//        override fun onClick(view: View) {
//            val position = adapterPosition
//            Log.d(TAG, "Complication onClick() position: $position")
//        }
//    }
//
//    /** Displays button to trigger background image complication selector.  */
//    inner class BackgroundComplicationViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
//
//        private val mBackgroundComplicationButton: Button
//
//        init {
//
//            mBackgroundComplicationButton = view.findViewById(R.id.background_complication_button) as Button
//            view.setOnClickListener(this)
//        }
//
//        fun setName(name: String) {
//            mBackgroundComplicationButton.text = name
//        }
//
//        fun setIcon(resourceId: Int) {
//            val context = mBackgroundComplicationButton.context
//            mBackgroundComplicationButton.setCompoundDrawablesWithIntrinsicBounds(
//                    context.getDrawable(resourceId), null, null, null)
//        }
//
//        override fun onClick(view: View) {
//            val position = adapterPosition
//            Log.d(TAG, "Background Complication onClick() position: $position")
//
//            val currentActivity = view.context as Activity
//
//            mSelectedComplicationId = YurisWatchface.getComplicationId(
//                    ComplicationLocation.BACKGROUND)
//
//            if (mSelectedComplicationId >= 0) {
//
//                val supportedTypes = YurisWatchface.getSupportedComplicationTypes(ComplicationLocation.BACKGROUND)
//
//                val watchFace = ComponentName(currentActivity, YurisWatchface::class.java)
//
//                currentActivity.startActivityForResult(
//                        ComplicationHelperActivity.createProviderChooserHelperIntent(
//                                currentActivity,
//                                watchFace,
//                                mSelectedComplicationId,
//                                *supportedTypes),
//                        ComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE)
//
//            } else {
//                Log.d(TAG, "Complication not supported by watch face.")
//            }
//        }
//    }
//
//    companion object {
//
//        private val TAG = "CompConfigAdapter"
//
//        val TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG = 0
//        val TYPE_MORE_OPTIONS = 1
//        val TYPE_COLOR_CONFIG = 2
//        val TYPE_UNREAD_NOTIFICATION_CONFIG = 3
//        val TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG = 4
//    }
//}
