// src/main/java/com/ecrea/potionmorepotion/PotionMorePotionMod.java
// 醸造レシピ登録部分 (commonSetup メソッド等) に以下を追加してください:

BrewingRecipeRegistry.addRecipe(new BetterBrewingRecipe(
        Potions.AWKWARD,
        Items.ENDER_PEARL,
        ModPotions.ENDER_PEARL_BLESSING.get()
));