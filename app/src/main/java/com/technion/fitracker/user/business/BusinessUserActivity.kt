package com.technion.fitracker.user.business

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.technion.fitracker.PendingRequestsActivity
import com.technion.fitracker.R
import com.technion.fitracker.SettingsActivity
import com.technion.fitracker.login.LoginActivity
import com.technion.fitracker.models.BusinessUserViewModel
import com.technion.fitracker.user.User
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class BusinessUserActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var navController: NavController
    private var auth: FirebaseAuth =  FirebaseAuth.getInstance()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var viewModel: BusinessUserViewModel

    private val topic1Name = "trainee_sent_request" + auth.currentUser!!.uid
    private val topic2Name = "trainee_accepted_trainer_request" + auth.currentUser!!.uid
    private val topic3Name = "appointment_canceletaion" + auth.currentUser!!.uid

    //Google login token
    private val idToken = "227928727350-8scqikjnk6ta5lj5runh2o0dbd9p0nil.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_business_user)
        setSupportActionBar(findViewById(R.id.business_user_toolbar))

        viewModel = ViewModelProviders.of(this)[BusinessUserViewModel::class.java]
        navController = Navigation.findNavController(findViewById(R.id.business_fragment_host))
        firestore = FirebaseFirestore.getInstance()


        if (auth.currentUser != null) {
            val docRef = firestore.collection("business_users").document(auth.currentUser!!.uid)
            docRef.get().addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                findViewById<TextView>(R.id.business_user_name).text = user?.name ?: "Username"

                viewModel.user_name = user?.name
                viewModel.user_photo_url = user?.photoURL
                viewModel.user_phone_number = user?.phone_number

                getUserPhoto()
            }
        }
        findViewById<BottomNavigationView>(R.id.business_bottom_navigation).setOnNavigationItemSelectedListener(this)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(idToken)
                .requestEmail()
                .build()
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(applicationContext, gso)


        createNotificationChannel()
        //subscribe to topics

        FirebaseMessaging.getInstance().subscribeToTopic(topic1Name)
        FirebaseMessaging.getInstance().subscribeToTopic(topic2Name)
        FirebaseMessaging.getInstance().subscribeToTopic(topic3Name)
    }


    override fun onStop() {
        super.onStop()
        //TODO: questionable, might be hurting performance ?
        viewModel.notifications_adapter?.stopListening()
    }

    private fun getUserPhoto() {
        val imagePath = File(this.filesDir, "/")
        val imageUserPath = File(imagePath, auth.currentUser?.uid!!)
        if(!imageUserPath.exists()){
            imageUserPath.mkdir()
        }
        val imageFile = File(imageUserPath, "profile_picture.jpg")
        if (imageFile.exists()) {
            Glide.with(this).load(imageFile.path).placeholder(R.drawable.user_avatar)
                    .error(R.drawable.user_avatar)
                    .skipMemoryCache(true) //2
                    .diskCacheStrategy(DiskCacheStrategy.NONE) //3
                    .transform(CircleCrop()) //4
                    .into(findViewById(R.id.business_user_avatar))
        } else {
            if (!viewModel.user_photo_url.isNullOrEmpty()) {
                Glide.with(this) //1
                        .load(viewModel.user_photo_url)
                        .placeholder(R.drawable.user_avatar)
                        .error(R.drawable.user_avatar)
                        .skipMemoryCache(true) //2
                        .diskCacheStrategy(DiskCacheStrategy.NONE) //3
                        .transform(CircleCrop()) //4
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                Log.d("GLIDE-ERROR", "Failed to load image")
                                return true
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.d("GLIDE-LOAD", "Loaded profile picture!")
                                saveProfilePicture(R.id.business_user_avatar)
                                return true
                            }

                        })
                        .into(findViewById(R.id.business_user_avatar))
            }
        }

    }

    private fun saveProfilePicture(drawableId:Int) {
        // Get the image from drawable resource as drawable object

        val bitmap = findViewById<ImageView>(drawableId).drawToBitmap()

        // Get the context wrapper instance
        val wrapper = ContextWrapper(applicationContext)

        // Initializing a new file
        // The bellow line return a directory in internal storage
        val imagePath = File(this.filesDir, "/")
        val imageUserPath = File(imagePath, auth.currentUser?.uid!!)
        if(!imageUserPath.exists()){
            imageUserPath.mkdir()
        }
        val imageFile = File(imageUserPath, "profile_picture.jpg")

        try {
            // Get the file output stream
            val stream: OutputStream = FileOutputStream(imageFile)

            // Compress bitmap
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            // Flush the stream
            stream.flush()

            // Close stream
            stream.close()
        } catch (e: IOException){ // Catch the exception
            e.printStackTrace()
        }

    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            val docRef = firestore.collection("business_users").document(auth.currentUser!!.uid)
            docRef.get().addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if(user?.photoURL != viewModel.user_photo_url){
                    val userAvatar = findViewById<ImageView>(R.id.business_user_avatar)
                    if (!user?.photoURL.isNullOrEmpty()) {
                        Glide.with(this) //1
                                .load(user?.photoURL)
                                .placeholder(R.drawable.user_avatar)
                                .error(R.drawable.user_avatar)
                                .skipMemoryCache(true) //2
                                .diskCacheStrategy(DiskCacheStrategy.NONE) //3
                                .transform(CircleCrop()) //4
                                .into(userAvatar)

                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.business_user_activity_menu, menu)
        return true

    }


    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        navController.popBackStack()
        when (menuItem.itemId) {
            R.id.action_home -> navController.navigate(R.id.homeScreenFragment)
            R.id.action_customers -> navController.navigate(R.id.customersFragment)
            R.id.action_schedule -> navController.navigate(R.id.scheduleFragment)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) { //check on which item the user pressed and perform the appropriate action
            R.id.business_user_menu_logout_ac -> {


                //unsubscribe from topics
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic1Name)
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic2Name)
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic3Name)

                FirebaseAuth.getInstance().signOut()
                mGoogleSignInClient.signOut()
                        .addOnCompleteListener(this) {
                            startLoginActivity()
                        }
                true
            }

            R.id.search_from_buisness -> {
                onSearchRequested()
                true
            }

            R.id.business_user_menu_settings_ac -> {
                val userHome = Intent(applicationContext, SettingsActivity::class.java)
                startActivity(userHome)
                true
            }
            R.id.business_user_menu_pending_requests_ac -> {
                val userHome = Intent(applicationContext, PendingRequestsActivity::class.java)
                userHome.putExtra("user_type", "business")

                userHome.putExtra("user_name", viewModel.user_name)
                userHome.putExtra("user_photo_url", viewModel.user_photo_url)

                startActivity(userHome)
                true
            }


            else -> {
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun startLoginActivity() {
        val userHome = Intent(applicationContext, LoginActivity::class.java)
        startActivity(userHome)
        finish()
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val id = "M_CH_ID"
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(id, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onSearchRequested(): Boolean {
        val appData = Bundle().apply {
            putString("user_type", "business")
            putString("user_name", viewModel.user_name)
            putString("user_photo_url", viewModel.user_photo_url)
            putString("user_phone_number", viewModel.user_phone_number)
        }
        startSearch(null, false, appData, false)
        return true
    }
}
