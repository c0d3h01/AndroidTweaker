package com.c0d3h01.androidtweaker.profiles

/** Stored profile IDs are stable; retired code 3 must not authorize a new boot action. */
enum class Profile(val code: Int, val commandKey: String, val preferenceKey: String = commandKey) {
  BATTERY(1, "battery"),
  BALANCED(2, "balanced"),
  PERFORMANCE(4, "performance", "gaming");

  companion object {
    fun fromCode(code: Int?): Profile? = entries.firstOrNull { it.code == code }
  }
}

fun profileLevel(code: Int?): Int = Profile.fromCode(code)?.let { it.ordinal + 1 } ?: 0
