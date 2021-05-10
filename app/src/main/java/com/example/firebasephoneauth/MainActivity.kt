package com.example.firebasephoneauth

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.firebasephoneauth.databinding.ActivityMainBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    //view Binding
    private lateinit var binding: ActivityMainBinding

    //if code sending failed, will use to resend
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null

    private var mCallBacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private var mVerificationId : String? = null
    private lateinit var firebaseAuth: FirebaseAuth

    private val TAG = "MAIN_TAG"

    //progress dialog
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.phoneL.visibility = View.VISIBLE
        binding.codeL.visibility = View.GONE

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        mCallBacks =object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                // this callback will be invoked in two situation:
                //1-- Instant verification. In some cases the phone number can be instantly verified
                //without needing to send or enter a verification code .
                //2-- Auto-retrieval. On some device Google play service can automatically detect the incoming
                //verification SMS and perform verification without user action
                Log.d(TAG, "onVerificationCompleted: ")
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
             // this call back is invoked in a valid request for verification is made.
              //for Instant if the phone number format is not valid
                progressDialog.dismiss()
                Log.d(TAG,"onVerificationFailed: ${e.message}")
                Toast.makeText(this@MainActivity, "${e.message}",Toast.LENGTH_SHORT).show()

            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
               //The SMS verification code has been sent to the provider phone number , we now need
                //to ask the user to enter the code and then construct a credential by combining
                //the code with a verification ID
                Log.d(TAG, "onCodeSent: $verificationId")
                mVerificationId = verificationId
                forceResendingToken = token
                progressDialog.dismiss()

                Log.d(TAG, "onCodeSent: $verificationId")

                //hide phone layout show code layout

                binding.phoneL.visibility = View.GONE
                binding.codeL.visibility = View.VISIBLE
                Toast.makeText(this@MainActivity, "Verification Code Sent...",Toast.LENGTH_SHORT).show()
                binding.codeSentDescriptionTv.text = "please type the verification code we sent to ${binding.phoneEt.text.toString().trim()}"

            }
        }
     //phoneContinueBtn click: input phone number , validate, start phone authentication/login
        binding.phoneButton.setOnClickListener {
       //input phone number
            val phone = binding.phoneEt.text.toString().trim()
            //validate phone number
            if (TextUtils.isEmpty(phone)){

                Toast.makeText(this@MainActivity, "please enter phone number", Toast.LENGTH_SHORT).show()
            }else{
                startPhoneNumberVerification(phone)
            }
        }

     //resendCodeTv click: (if code didn't received) resend verification code/OTP
        binding.resendCodeTv.setOnClickListener {
            //input phone number
            val phone = binding.phoneEt.text.toString().trim()
            //validate phone number
            if (TextUtils.isEmpty(phone)) {

                Toast.makeText(this@MainActivity, "please enter phone number", Toast.LENGTH_SHORT).show()
            } else {
                resendVerificationCode(phone, forceResendingToken)                    //!!adding
            }
        }

     //codeSubmitBtn click: input verification code, validate, verify phone number with verification code
        binding.codeSubmitButton.setOnClickListener {
            //input verification code
            val code = binding.codeEt.text.toString().trim()
            if (TextUtils.isEmpty(code)){
                Toast.makeText(this@MainActivity, "please enter verification code", Toast.LENGTH_SHORT).show()
            }else{
                verifyPhoneNumberWithCode(mVerificationId, code)
            }

        }
    }

    private fun startPhoneNumberVerification(phone: String){
        Log.d(TAG, "startPhoneNumberVerification: $phone")
        progressDialog.setMessage("Verifying Phone Number... ")
        progressDialog.show()

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallBacks)
                .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(phone: String, token: PhoneAuthProvider.ForceResendingToken?) {
        progressDialog.setMessage("Resending Code... ")
        progressDialog.show()

        Log.d(TAG, "resendVerificationCode: $phone")

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallBacks)
                .setForceResendingToken(token)
                .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code:String){
        Log.d(TAG, "verifyPhoneNumberWithCode: $verificationId $code")
        progressDialog.setMessage("Verifying Code... ")
        progressDialog.show()

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "signInWithPhoneAuthCredential: ")
        progressDialog.setMessage("Loggin In")

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener {
                    //login success
                    progressDialog.dismiss()
                    val phone = firebaseAuth.currentUser.phoneNumber
                    Toast.makeText(this, "Logged In as $phone",Toast.LENGTH_SHORT).show()

                    //start profile activity

                    startActivity(Intent(this,ProfileActivity::class.java))
                    finish()

                }
                .addOnFailureListener {e->
                    //login failed
                    progressDialog.dismiss()
                    Toast.makeText(this, "${e.message}",Toast.LENGTH_SHORT).show()

                }
         }

}