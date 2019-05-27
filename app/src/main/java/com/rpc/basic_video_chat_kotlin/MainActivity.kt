package com.rpc.basic_video_chat_kotlin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.opengl.GLSurfaceView
import android.Manifest
import android.util.Log
import android.widget.FrameLayout
import android.app.AlertDialog
import android.os.Handler
import android.widget.Toast

import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Subscriber
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.SubscriberKit

import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks,
    Session.SessionListener, PublisherKit.PublisherListener, SubscriberKit.SubscriberListener {

    private var mSession: Session? = null
    private var mPublisher: Publisher? = null
    private var mSubscriber: Subscriber? = null

    private var mPublisherViewContainer: FrameLayout? = null
    private var mSubscriberViewContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        Log.d(LOG_TAG, "onCreate")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize view objects from your layout
        mPublisherViewContainer = findViewById(R.id.publisher_container)
        mSubscriberViewContainer = findViewById(R.id.subscriber_container)

        requestPermissions()
    }

    /* Activity lifecycle methods */

    override fun onPause() {

        Log.d(LOG_TAG, "onPause")

        super.onPause()

        if (mSession != null) {
            mSession!!.onPause()
        }

    }

    override fun onResume() {

        Log.d(LOG_TAG, "onResume")

        super.onResume()

        if (mSession != null) {
            mSession!!.onResume()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {

        Log.d(LOG_TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {

        Log.d(LOG_TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size)

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this)
                .setTitle(getString(R.string.title_settings_dialog))
                .setRationale(getString(R.string.rationale_ask_again))
                .setPositiveButton(getString(R.string.setting))
                .setNegativeButton(getString(R.string.cancel))
                .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                .build()
                .show()
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private fun requestPermissions() {

        val perms = arrayOf(Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(MainActivity@this, *perms)) {
            initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN)
        } else EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, *perms)
    }

    private fun initializeSession(apiKey: String, sessionId: String, token: String) {

        mSession = Session.Builder(this, apiKey, sessionId).build()
        mSession!!.setSessionListener(this)
        mSession!!.connect(token)
    }

    /* Session Listener methods */

    override fun onConnected(session: Session) {

        Log.d(LOG_TAG, "onConnected: Connected to session: " + session.sessionId)

        // initialize Publisher and set this object to listen to Publisher events
        mPublisher = Publisher.Builder(this).build()
        mPublisher!!.setPublisherListener(this)

        // set publisher video style to fill view
        mPublisher!!.renderer.setStyle(
            BaseVideoRenderer.STYLE_VIDEO_SCALE,
            BaseVideoRenderer.STYLE_VIDEO_FILL
        )
        mPublisherViewContainer!!.addView(mPublisher!!.view)
        if (mPublisher!!.view is GLSurfaceView) {
            (mPublisher!!.view as GLSurfaceView).setZOrderOnTop(true)
        }

        mSession!!.publish(mPublisher)
    }

    override fun onDisconnected(session: Session) {

        Log.d(LOG_TAG, "onDisconnected: Disconnected from session: " + session.sessionId)
    }

    override fun onStreamReceived(session: Session, stream: Stream) {

        Log.d(LOG_TAG, "onStreamReceived: New Stream Received " + stream.streamId + " in session: " + session.sessionId)

        if (mSubscriber == null) {
            mSubscriber = Subscriber.Builder(this, stream).build()
            mSubscriber!!.renderer.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            mSubscriber!!.setSubscriberListener(this)
            mSession!!.subscribe(mSubscriber)
            mSubscriberViewContainer!!.addView(mSubscriber!!.view)
        }
    }

    override fun onStreamDropped(session: Session, stream: Stream) {

        Log.d(LOG_TAG, "onStreamDropped: Stream Dropped: " + stream.streamId + " in session: " + session.sessionId)

        if (mSubscriber != null) {
            mSubscriber = null
            mSubscriberViewContainer!!.removeAllViews()
        }
    }

    override fun onError(session: Session, opentokError: OpentokError) {
        Log.e(
            LOG_TAG, "onError: " + opentokError.errorDomain + " : " +
                    opentokError.errorCode + " - " + opentokError.message + " in session: " + session.sessionId
        )

        showOpenTokError(opentokError)
    }

    /* Publisher Listener methods */

    override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {

        Log.d(LOG_TAG, "onStreamCreated: Publisher Stream Created. Own stream " + stream.streamId)
    }

    override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {

        Log.d(LOG_TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream " + stream.streamId)
    }

    override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {

        Log.e(
            LOG_TAG, "onError: " + opentokError.errorDomain + " : " +
                    opentokError.errorCode + " - " + opentokError.message
        )

        showOpenTokError(opentokError)
    }

    override fun onConnected(subscriberKit: SubscriberKit) {

        Log.d(LOG_TAG, "onConnected: Subscriber connected. Stream: " + subscriberKit.stream.streamId)
    }

    override fun onDisconnected(subscriberKit: SubscriberKit) {

        Log.d(LOG_TAG, "onDisconnected: Subscriber disconnected. Stream: " + subscriberKit.stream.streamId)
    }

    override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {

        Log.e(
            LOG_TAG, "onError: " + opentokError.errorDomain + " : " +
                    opentokError.errorCode + " - " + opentokError.message
        )

        showOpenTokError(opentokError)
    }

    private fun showOpenTokError(opentokError: OpentokError) {
        Toast.makeText(
            this,
            opentokError.errorDomain.name + ": " + opentokError.message + " Please, see the logcat.",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun showConfigError(alertTitle: String, errorMessage: String) {
        Log.e(LOG_TAG, "Error $alertTitle: $errorMessage")
        AlertDialog.Builder(this)
            .setTitle(alertTitle)
            .setMessage(errorMessage)
            .setPositiveButton(
                "ok"
            ) { _, _ -> this@MainActivity.finish() }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    companion object {

        private val LOG_TAG = MainActivity::class.java.simpleName
        private const val RC_SETTINGS_SCREEN_PERM = 123
        private const val RC_VIDEO_APP_PERM = 124
    }
}

