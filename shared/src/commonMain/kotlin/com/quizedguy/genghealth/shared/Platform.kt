package com.quizedguy.genghealth.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
