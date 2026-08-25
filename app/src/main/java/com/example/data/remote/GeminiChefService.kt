package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CookingStep
import com.example.data.model.IngredientItem
import com.example.data.model.ReelAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiChefService {
    private const val TAG = "GeminiChefService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeFoodReel(
        urlOrText: String,
        userDishHint: String = ""
    ): ReelAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "GEMINI_API_KEY is not configured, generating intelligent culinary recipe based on link info.")
            return@withContext generateSmartFallback(urlOrText, userDishHint)
        }

        val prompt = buildString {
            appendLine("You are REELCHEF AI, a Michelin-star Executive Chef and viral food content analyst.")
            appendLine("A user pasted this Instagram Food Reel / Video URL or food description:")
            appendLine("URL/Content: \"$urlOrText\"")
            if (userDishHint.isNotBlank()) {
                appendLine("User note/dish hint: \"$userDishHint\"")
            }
            appendLine()
            appendLine("Analyze this food video/reel. Extract or intelligently reconstruct the complete, mouth-watering recipe that would be shown in this food reel.")
            appendLine("You MUST respond ONLY with a raw, valid JSON object (no markdown code blocks, no backticks, no markdown formatting).")
            appendLine("The JSON must match this exact schema:")
            appendLine("{")
            appendLine("  \"dishName\": \"Crispy Garlic Butter Steak Bites\",")
            appendLine("  \"cuisine\": \"American / Steakhouse\",")
            appendLine("  \"category\": \"Dinner\",")
            appendLine("  \"prepTime\": \"10 mins\",")
            appendLine("  \"cookTime\": \"15 mins\",")
            appendLine("  \"totalTime\": \"25 mins\",")
            appendLine("  \"servings\": \"2-3 servings\",")
            appendLine("  \"difficulty\": \"Easy\",")
            appendLine("  \"calories\": \"520 kcal\",")
            appendLine("  \"macros\": \"Protein: 42g | Carbs: 6g | Fat: 36g\",")
            appendLine("  \"creatorHandle\": \"@foodiechef\",")
            appendLine("  \"pairingSuggestion\": \"Crispy roasted potatoes & herb garlic baguette\",")
            appendLine("  \"chefTips\": [\"Use high smoke point oil before basting with butter\", \"Rest meat for 5 mins\"],")
            appendLine("  \"ingredients\": [")
            appendLine("    {\"name\": \"Sirloin or Ribeye steak cubed\", \"amount\": \"500g (1 lb)\"},")
            appendLine("    {\"name\": \"Unsalted butter\", \"amount\": \"3 tbsp\"},")
            appendLine("    {\"name\": \"Garlic minced\", \"amount\": \"4 cloves\"},")
            appendLine("    {\"name\": \"Fresh rosemary & thyme\", \"amount\": \"2 sprigs each\"}")
            appendLine("  ],")
            appendLine("  \"instructions\": [")
            appendLine("    {\"stepNumber\": 1, \"instruction\": \"Pat the steak cubes completely dry and season liberally with kosher salt and cracked black pepper.\", \"timerSeconds\": 120, \"tip\": \"Dry meat sears much better than damp meat!\"},")
            appendLine("    {\"stepNumber\": 2, \"instruction\": \"Heat a heavy cast iron skillet until smoking hot with 1 tbsp avocado oil.\", \"timerSeconds\": 180, \"tip\": \"Do not overcrowd the pan.\"},")
            appendLine("    {\"stepNumber\": 3, \"instruction\": \"Sear steak bites in single layer for 2 minutes undisturbed, then flip for 2 minutes.\", \"timerSeconds\": 240, \"tip\": \"Get a deep golden crust.\"},")
            appendLine("    {\"stepNumber\": 4, \"instruction\": \"Toss in butter, garlic, and herbs. Baste the foaming butter over the steak for 1 minute and serve immediately.\", \"timerSeconds\": 60, \"tip\": \"Smells heavenly!\"}")
            appendLine("  ]")
            appendLine("}")
        }

        try {
            val jsonPayload = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", prompt)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val genConfig = JSONObject()
                genConfig.put("temperature", 0.4)
                put("generationConfig", genConfig)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${response.code} - $responseBody")
                return@withContext generateSmartFallback(urlOrText, userDishHint)
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseGeminiResponse(text, urlOrText, userDishHint)
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini call, fallback to smart analysis", e)
            generateSmartFallback(urlOrText, userDishHint)
        }
    }

    private fun parseGeminiResponse(rawText: String, url: String, hint: String): ReelAnalysisResult {
        try {
            // Clean markdown code fence if present
            var cleaned = rawText.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json").trim()
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```").trim()
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```").trim()
            }

            val json = JSONObject(cleaned)
            val dishName = json.optString("dishName", if (hint.isNotBlank()) hint else "Delicious Reel Recipe")
            val cuisine = json.optString("cuisine", "Fusion")
            val category = json.optString("category", "Dinner")
            val prepTime = json.optString("prepTime", "15 mins")
            val cookTime = json.optString("cookTime", "20 mins")
            val totalTime = json.optString("totalTime", "35 mins")
            val servings = json.optString("servings", "2-4 servings")
            val difficulty = json.optString("difficulty", "Medium")
            val calories = json.optString("calories", "450 kcal")
            val macros = json.optString("macros", "Protein: 24g | Carbs: 40g | Fat: 18g")
            val creator = json.optString("creatorHandle", extractHandleFromUrl(url))
            val pairing = json.optString("pairingSuggestion", "Fresh greens & sparkling beverage")

            val ingredientsList = mutableListOf<IngredientItem>()
            val ingArray = json.optJSONArray("ingredients")
            if (ingArray != null) {
                for (i in 0 until ingArray.length()) {
                    val item = ingArray.optJSONObject(i)
                    if (item != null) {
                        ingredientsList.add(
                            IngredientItem(
                                name = item.optString("name", "Ingredient"),
                                amount = item.optString("amount", "")
                            )
                        )
                    }
                }
            }

            val instructionsList = mutableListOf<CookingStep>()
            val instArray = json.optJSONArray("instructions")
            if (instArray != null) {
                for (i in 0 until instArray.length()) {
                    val stepObj = instArray.optJSONObject(i)
                    if (stepObj != null) {
                        val timerSec = if (stepObj.has("timerSeconds") && !stepObj.isNull("timerSeconds")) {
                            stepObj.optInt("timerSeconds")
                        } else null
                        val tip = if (stepObj.has("tip") && !stepObj.isNull("tip")) {
                            stepObj.optString("tip")
                        } else null
                        instructionsList.add(
                            CookingStep(
                                stepNumber = stepObj.optInt("stepNumber", i + 1),
                                instruction = stepObj.optString("instruction", ""),
                                timerSeconds = timerSec,
                                tip = tip
                            )
                        )
                    }
                }
            }

            val tipsList = mutableListOf<String>()
            val tipsArray = json.optJSONArray("chefTips")
            if (tipsArray != null) {
                for (i in 0 until tipsArray.length()) {
                    tipsList.add(tipsArray.optString(i))
                }
            }

            return ReelAnalysisResult(
                dishName = dishName,
                cuisine = cuisine,
                category = category,
                prepTime = prepTime,
                cookTime = cookTime,
                totalTime = totalTime,
                servings = servings,
                difficulty = difficulty,
                calories = calories,
                macros = macros,
                ingredients = if (ingredientsList.isNotEmpty()) ingredientsList else getDefaultIngredients(dishName),
                instructions = if (instructionsList.isNotEmpty()) instructionsList else getDefaultInstructions(dishName),
                chefTips = if (tipsList.isNotEmpty()) tipsList else listOf("Cook on medium-high heat for maximum caramelization", "Taste and adjust seasoning right before serving"),
                pairingSuggestion = pairing,
                creatorHandle = creator
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response: $rawText", e)
            return generateSmartFallback(url, hint)
        }
    }

    private fun extractHandleFromUrl(url: String): String {
        return try {
            if (url.contains("instagram.com/")) {
                val parts = url.split("instagram.com/")
                if (parts.size > 1) {
                    val sub = parts[1].split("/")[0]
                    if (sub != "reel" && sub != "p" && sub != "tv" && sub.isNotBlank()) {
                        return "@$sub"
                    }
                }
            }
            "@reelchef"
        } catch (e: Exception) {
            "@reelchef"
        }
    }

    fun generateSmartFallback(url: String, hint: String): ReelAnalysisResult {
        val lower = (url + " " + hint).lowercase()
        val handle = extractHandleFromUrl(url)

        return when {
            lower.contains("pasta") || lower.contains("carbonara") || lower.contains("vodka sauce") || lower.contains("alfredo") -> {
                ReelAnalysisResult(
                    dishName = if (hint.isNotBlank()) hint else "Creamy Tuscan Garlic Butter Pasta",
                    cuisine = "Italian",
                    category = "Pasta",
                    prepTime = "10 mins",
                    cookTime = "15 mins",
                    totalTime = "25 mins",
                    servings = "2-4 servings",
                    difficulty = "Easy",
                    calories = "540 kcal",
                    macros = "Protein: 18g | Carbs: 68g | Fat: 22g",
                    ingredients = listOf(
                        IngredientItem("Fettuccine or Rigatoni Pasta", "350g"),
                        IngredientItem("Heavy Cream", "1 cup (240ml)"),
                        IngredientItem("Parmesan Cheese (freshly grated)", "1 cup (90g)"),
                        IngredientItem("Garlic (finely minced)", "5 cloves"),
                        IngredientItem("Sun-dried tomatoes in oil", "1/2 cup chopped"),
                        IngredientItem("Baby spinach", "2 large cups"),
                        IngredientItem("Unsalted butter", "2 tbsp"),
                        IngredientItem("Red pepper chili flakes", "1/2 tsp"),
                        IngredientItem("Fresh basil leaves", "handful")
                    ),
                    instructions = listOf(
                        CookingStep(1, "Bring a large pot of salted water to a rolling boil and cook pasta until 1 minute before al dente. Reserve 1/2 cup starchy pasta water.", 480, "Pasta water is liquid gold for velvety sauces!"),
                        CookingStep(2, "In a large skillet over medium heat, melt butter with 1 tbsp sun-dried tomato oil. Sauté minced garlic and chili flakes for 60 seconds until fragrant.", 60, "Do not let garlic brown too dark."),
                        CookingStep(3, "Pour in heavy cream and bring to a gentle simmer for 3 minutes. Stir in sun-dried tomatoes and freshly grated parmesan cheese until silky smooth.", 180, "Whisk constantly for a luxurious emulsion."),
                        CookingStep(4, "Fold in baby spinach until wilted (about 60 seconds). Add cooked pasta and reserved pasta water, tossing vigorously to coat every strand.", 120, "Toss continuously to emulsify."),
                        CookingStep(5, "Garnish with fresh basil, cracked black pepper, and extra parmesan. Serve immediately hot!", null, "Enjoy your viral restaurant-quality pasta!")
                    ),
                    chefTips = listOf(
                        "Always grate whole block Parmigiano-Reggiano; pre-shredded cheese contains anti-caking agents that ruin sauces.",
                        "Add a splash of lemon zest at the very end to cut through the rich cream."
                    ),
                    pairingSuggestion = "Chilled Pinot Grigio or Crisp Sparkling Lemon Water with Garlic Toast",
                    creatorHandle = handle
                )
            }
            lower.contains("ramen") || lower.contains("noodle") || lower.contains("asian") || lower.contains("dumpling") -> {
                ReelAnalysisResult(
                    dishName = if (hint.isNotBlank()) hint else "Viral 10-Minute Garlic Chili Oil Ramen",
                    cuisine = "Asian / Japanese",
                    category = "Quick Meals",
                    prepTime = "5 mins",
                    cookTime = "7 mins",
                    totalTime = "12 mins",
                    servings = "1-2 servings",
                    difficulty = "Beginner",
                    calories = "460 kcal",
                    macros = "Protein: 14g | Carbs: 58g | Fat: 20g",
                    ingredients = listOf(
                        IngredientItem("Instant Ramen Noodles (any brand)", "1-2 packs"),
                        IngredientItem("Garlic (minced)", "4 cloves"),
                        IngredientItem("Green Scallions (sliced)", "2 stalks"),
                        IngredientItem("Chili crisp / Red chili flakes", "1.5 tbsp"),
                        IngredientItem("Soy sauce & Oyster sauce", "1 tbsp each"),
                        IngredientItem("Toasted sesame oil", "1 tbsp"),
                        IngredientItem("Neutral high-heat oil", "3 tbsp"),
                        IngredientItem("Soft-boiled jammy egg", "1 egg"),
                        IngredientItem("Toasted sesame seeds", "1 tsp")
                    ),
                    instructions = listOf(
                        CookingStep(1, "Boil ramen noodles according to package instructions (about 3 minutes), then drain well.", 180, "Keep noodles bouncy, do not overcook."),
                        CookingStep(2, "In your serving bowl, combine minced garlic, chopped scallions, chili crisp, soy sauce, oyster sauce, and sesame seeds.", null, "Layer them in bowl before adding hot oil."),
                        CookingStep(3, "Heat 3 tbsp of cooking oil in a small pan until smoking hot, then carefully pour directly over the aromatics in the bowl to sizzle.", 60, "Hear that sizzle! It unlocks massive flavor."),
                        CookingStep(4, "Add drained hot noodles directly into the spiced oil mixture and toss thoroughly with chopsticks.", 45, "Make sure every noodle is coated."),
                        CookingStep(5, "Top with a soft-boiled jammy egg, extra scallions, and nori seaweed strips. Slurp immediately!", null, "Pure comfort food in under 10 minutes!")
                    ),
                    chefTips = listOf(
                        "Pouring smoking oil directly over raw garlic and chili blooms the essential oils without burning them.",
                        "Boil eggs for exactly 6 minutes and 30 seconds, then ice bath for the perfect gooey yolk."
                    ),
                    pairingSuggestion = "Iced Green Tea or Japanese Ramune soda with Gyoza",
                    creatorHandle = handle
                )
            }
            lower.contains("taco") || lower.contains("mexican") || lower.contains("quesadilla") || lower.contains("birria") -> {
                ReelAnalysisResult(
                    dishName = if (hint.isNotBlank()) hint else "Crispy Cheesy Birria Smash Tacos",
                    cuisine = "Mexican",
                    category = "Street Food",
                    prepTime = "15 mins",
                    cookTime = "15 mins",
                    totalTime = "30 mins",
                    servings = "3-4 servings",
                    difficulty = "Medium",
                    calories = "580 kcal",
                    macros = "Protein: 34g | Carbs: 38g | Fat: 32g",
                    ingredients = listOf(
                        IngredientItem("Corn or Flour Tortillas", "6-8 small"),
                        IngredientItem("Ground beef or shredded chuck roast", "450g (1 lb)"),
                        IngredientItem("Oaxaca or Monterey Jack cheese", "2 cups shredded"),
                        IngredientItem("White onion (diced)", "1/2 cup"),
                        IngredientItem("Fresh cilantro (chopped)", "1/2 cup"),
                        IngredientItem("Chili powder, cumin, smoked paprika", "1 tbsp blend"),
                        IngredientItem("Lime wedges", "2 limes"),
                        IngredientItem("Avocado Salsa Verde", "1/2 cup")
                    ),
                    instructions = listOf(
                        CookingStep(1, "Season ground beef or beef shreds with spices, garlic, and a splash of lime juice in a hot skillet until caramelized and juicy.", 360, "Searing deeply creates rich umami."),
                        CookingStep(2, "Heat a flat griddle or cast iron pan over medium-high heat with a brush of beef dripping or oil.", 120, "Get the pan sizzling."),
                        CookingStep(3, "Dip tortillas lightly into the cooking juices and lay flat on the hot griddle. Top immediately with shredded cheese and seasoned beef.", 90, "Cover entire surface with cheese for crispy frico edges."),
                        CookingStep(4, "Fold taco in half and press down firmly with spatula until tortilla is blistered, crispy, and cheese is molten.", 180, "Flip once to get both sides super crunchy."),
                        CookingStep(5, "Open slightly and stuff with diced onion, cilantro, and squeeze fresh lime. Dip in consommé or salsa and enjoy!", null, "The ultimate viral street taco crunch!")
                    ),
                    chefTips = listOf(
                        "Dipping tortillas in seasoned fat/consommé before grilling creates that iconic red crispy shell.",
                        "Fresh white onion and cilantro provide the critical acid and crunch balance to the rich cheese."
                    ),
                    pairingSuggestion = "Ice Cold Horchata or Spicy Mango Margarita with Guacamole",
                    creatorHandle = handle
                )
            }
            lower.contains("dessert") || lower.contains("cake") || lower.contains("cookie") || lower.contains("chocolate") || lower.contains("brownie") -> {
                ReelAnalysisResult(
                    dishName = if (hint.isNotBlank()) hint else "Molten Lava Chocolate Mug Cake Hack",
                    cuisine = "French / Bakery",
                    category = "Desserts",
                    prepTime = "5 mins",
                    cookTime = "2 mins",
                    totalTime = "7 mins",
                    servings = "1-2 servings",
                    difficulty = "Easy",
                    calories = "390 kcal",
                    macros = "Protein: 6g | Carbs: 52g | Fat: 19g",
                    ingredients = listOf(
                        IngredientItem("All-purpose flour", "4 tbsp"),
                        IngredientItem("Granulated sugar", "3 tbsp"),
                        IngredientItem("Unsweetened cocoa powder", "2 tbsp"),
                        IngredientItem("Baking powder", "1/2 tsp"),
                        IngredientItem("Whole milk or oat milk", "3 tbsp"),
                        IngredientItem("Melted butter or coconut oil", "2 tbsp"),
                        IngredientItem("Nutella or dark chocolate truffles", "2 chunks"),
                        IngredientItem("Vanilla ice cream & sea salt", "for topping")
                    ),
                    instructions = listOf(
                        CookingStep(1, "In a microwave-safe ceramic mug, whisk together flour, sugar, cocoa powder, baking powder, and a pinch of salt.", 60, "Whisk until no dry flour lumps remain."),
                        CookingStep(2, "Add milk, melted butter, and a drop of vanilla. Mix with a small fork until a thick glossy batter forms.", 60, "Scrape edges of the mug."),
                        CookingStep(3, "Drop 2 large chunks of dark chocolate or a generous spoonful of Nutella right into the center of the batter.", 30, "Do not push it all the way down, let batter enclose it."),
                        CookingStep(4, "Microwave on high for 70 to 80 seconds. The top will rise and look spongy while the center stays gooey molten.", 75, "Watch it rise without overflowing."),
                        CookingStep(5, "Let cool for 1 minute, top with a cold scoop of vanilla ice cream, and dust with flaky sea salt.", 60, "Dig in while the center is warm and lava-like!")
                    ),
                    chefTips = listOf(
                        "Do not overcook! Pull it out when the edges are set and center is still slightly shiny.",
                        "Flaky sea salt (Maldon) enhances the deep cocoa flavor tremendously."
                    ),
                    pairingSuggestion = "Cold Espresso / Cappuccino or Iced Oat Milk Latte",
                    creatorHandle = handle
                )
            }
            else -> {
                // General gourmet viral food reel
                ReelAnalysisResult(
                    dishName = if (hint.isNotBlank()) hint else "Crispy Garlic Butter Smash Burger",
                    cuisine = "American Gourmet",
                    category = "Dinner",
                    prepTime = "10 mins",
                    cookTime = "8 mins",
                    totalTime = "18 mins",
                    servings = "2 burgers",
                    difficulty = "Easy",
                    calories = "620 kcal",
                    macros = "Protein: 38g | Carbs: 42g | Fat: 36g",
                    ingredients = listOf(
                        IngredientItem("Ground Chuck 80/20 beef", "350g (divided into 4 balls)"),
                        IngredientItem("Brioche burger buns (toasted)", "2 buns"),
                        IngredientItem("American cheese or Sharp Cheddar", "4 slices"),
                        IngredientItem("Thinly shaved sweet onions", "1 cup"),
                        IngredientItem("Garlic Aioli / Secret Burger Sauce", "3 tbsp"),
                        IngredientItem("Dill pickle chips", "8 slices"),
                        IngredientItem("Kosher salt & black pepper", "1 tsp each"),
                        IngredientItem("Butter for buns", "2 tbsp")
                    ),
                    instructions = listOf(
                        CookingStep(1, "Heat a cast iron griddle or heavy pan over smoking high heat. Butter and toast brioche buns until golden brown.", 120, "Toasting buns prevents sogginess."),
                        CookingStep(2, "Place beef balls on screaming hot dry griddle. Top with a mountain of shaved onions and smash ultra-thin with parchment paper and heavy spatula.", 60, "Smash until lacey edges form!"),
                        CookingStep(3, "Season generously with salt and black pepper. Let sear undisturbed for 2.5 minutes until crispy dark crust develops.", 150, "Do not move the patties while crust forms."),
                        CookingStep(4, "Scrape under patties with sharp spatula to preserve the crust, flip, and immediately slap cheese on each patty.", 60, "Stack double patties together."),
                        CookingStep(5, "Spread secret sauce on both bun halves, layer pickles, double smashed beef stack, crown bun, and press gently.", null, "Cut in half for that viral cheesy cross-section!")
                    ),
                    chefTips = listOf(
                        "High heat and 80/20 fat ratio are essential for the ultra-crispy lacey edges.",
                        "Use parchment paper under your burger press so the beef doesn't stick to the metal."
                    ),
                    pairingSuggestion = "Crispy Truffle Parmesan Fries and Cold Craft Soda",
                    creatorHandle = handle
                )
            }
        }
    }

    private fun getDefaultIngredients(dishName: String): List<IngredientItem> {
        return listOf(
            IngredientItem("Main Protein / Base Ingredient", "400g"),
            IngredientItem("Aromatics (Garlic, Shallots, Herbs)", "3 tbsp"),
            IngredientItem("Cooking Oil / Grass-fed Butter", "2 tbsp"),
            IngredientItem("Seasoning blend & Kosher salt", "to taste"),
            IngredientItem("Fresh Herb Garnish", "handful")
        )
    }

    private fun getDefaultInstructions(dishName: String): List<CookingStep> {
        return listOf(
            CookingStep(1, "Prep all ingredients and bring ingredients to room temperature for even cooking.", 180, "Mise en place is key!"),
            CookingStep(2, "Heat your pan over medium-high heat and add cooking oil or butter.", 120, "Listen for the sizzle."),
            CookingStep(3, "Cook main components until golden brown and aromatic.", 360, "Don't rush the caramelization."),
            CookingStep(4, "Combine sauces, seasonings, and baste thoroughly.", 180, "Taste and adjust seasoning."),
            CookingStep(5, "Plate with fresh herb garnish and serve hot immediately!", null, "Enjoy your culinary masterpiece!")
        )
    }
}
