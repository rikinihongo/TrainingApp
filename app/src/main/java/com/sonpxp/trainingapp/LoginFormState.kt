package com.sonpxp.trainingapp

/**
 * Created by Sonpx on 12/25/2024
 * Copyright(©)Cloudxanh. All rights reserved.
 */

/**
 * Data validation state of the login form.
 */
sealed class LoginFormState

data class FailedLoginFormState(
    val usernameError: Int? = null,
    val passwordError: Int? = null
) : LoginFormState()

data class SuccessfulLoginFormState(
    val isDataValid: Boolean = false
) : LoginFormState()