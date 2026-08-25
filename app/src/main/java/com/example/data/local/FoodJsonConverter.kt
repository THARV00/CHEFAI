package com.example.data.local

import com.example.data.model.CookingStep
import com.example.data.model.IngredientItem
import org.json.JSONArray
import org.json.JSONObject

object FoodJsonConverter {

    fun ingredientsToJson(ingredients: List<IngredientItem>): String {
        val array = JSONArray()
        for (item in ingredients) {
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("amount", item.amount)
            obj.put("isChecked", item.isChecked)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToIngredients(json: String): List<IngredientItem> {
        val list = mutableListOf<IngredientItem>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    IngredientItem(
                        name = obj.optString("name", ""),
                        amount = obj.optString("amount", ""),
                        isChecked = obj.optBoolean("isChecked", false)
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    fun instructionsToJson(instructions: List<CookingStep>): String {
        val array = JSONArray()
        for (step in instructions) {
            val obj = JSONObject()
            obj.put("stepNumber", step.stepNumber)
            obj.put("instruction", step.instruction)
            if (step.timerSeconds != null) {
                obj.put("timerSeconds", step.timerSeconds)
            }
            if (step.tip != null) {
                obj.put("tip", step.tip)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToInstructions(json: String): List<CookingStep> {
        val list = mutableListOf<CookingStep>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val timerSec = if (obj.has("timerSeconds")) obj.optInt("timerSeconds") else null
                val tip = if (obj.has("tip")) obj.optString("tip") else null
                list.add(
                    CookingStep(
                        stepNumber = obj.optInt("stepNumber", i + 1),
                        instruction = obj.optString("instruction", ""),
                        timerSeconds = timerSec,
                        tip = tip
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    fun stringsToJson(list: List<String>): String {
        val array = JSONArray()
        for (item in list) {
            array.put(item)
        }
        return array.toString()
    }

    fun jsonToStrings(json: String): List<String> {
        val list = mutableListOf<String>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}
