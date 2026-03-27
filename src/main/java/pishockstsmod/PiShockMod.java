package pishockstsmod;

import basemod.AutoAdd;
import basemod.BaseMod;
import basemod.interfaces.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.GameOverScreen;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import pishockstsmod.cards.BaseCard;
import pishockstsmod.relics.BaseRelic;
import pishockstsmod.util.GeneralUtils;
import pishockstsmod.util.KeywordInfo;
import pishockstsmod.util.TextureLoader;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scannotation.AnnotationDB;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SpireInitializer
public class PiShockMod implements
        PostDeathSubscriber,
        PostCreateStartingRelicsSubscriber,
        EditCardsSubscriber,
        EditRelicsSubscriber,
        EditStringsSubscriber,
        EditKeywordsSubscriber,
        PostInitializeSubscriber {
    public static ModInfo info;
    public static String modID; //Edit your pom.xml to change this
    static { loadModInfo(); }
    private static final String resourcesFolder = checkResourcesPath();
    public static final Logger logger = LogManager.getLogger(modID); //Used to output to the console.

    public static String USERNAME = "undefined";
    public static String APIKEY = "undefined";
    public static String SHOCKERID = "undefined";

    public static int MINIMUM_PIZAP_DAMAGE = 1;
    public static int MAXIMUM_PIZAP_DAMAGE = 100;
    public static int MINIMUM_PIZAP_LENGTH = 1;
    public static int MAXIMUM_PIZAP_LENGTH = 8;

    public static boolean OLD_SHARECODE_DETECTED = false;

    public static boolean INVALID_PISHOCK_DATA = false;

    //This is used to prefix the IDs of various objects like cards and relics,
    //to avoid conflicts between different mods using the same name for things.
    public static String makeID(String id) {
        return modID + ":" + id;
    }

    //This will be called by ModTheSpire because of the @SpireInitializer annotation at the top of the class.
    public static void initialize() {
        new PiShockMod();
    }

    public PiShockMod() {
        BaseMod.subscribe(this); //This will make BaseMod trigger all the subscribers at their appropriate times.
        logger.info(modID + " subscribed to BaseMod.");

        File piShockConfigFile = new File("pishockconfig.txt");
        if(piShockConfigFile.exists() && !piShockConfigFile.isDirectory()){
            logger.info("Loading preferences stored at pishockconfig.txt");
            loadConfigFile(piShockConfigFile);
        }
        else{
            logger.info("Generating pishockconfig.txt file");
            generateConfigFile(piShockConfigFile);
        }

        logger.info("User {} (APIKEY: {}) is using ShockerID {}.",USERNAME, APIKEY, SHOCKERID);

        if(Objects.equals(APIKEY, "undefined") || Objects.equals(SHOCKERID, "undefined")){
            INVALID_PISHOCK_DATA = true;
            logger.info("Missing PiShock account data. PiShock requests have been disabled");
        }
        else if(MINIMUM_PIZAP_DAMAGE < 1 || MAXIMUM_PIZAP_DAMAGE > 100 || MINIMUM_PIZAP_LENGTH < 1 || MINIMUM_PIZAP_DAMAGE > MAXIMUM_PIZAP_DAMAGE || MINIMUM_PIZAP_LENGTH > MAXIMUM_PIZAP_LENGTH){
            INVALID_PISHOCK_DATA = true;
            logger.info("PiShock safety values were not set properly. PiShock requests have been disabled.");
        }
        else{
            logger.info("PiShock safety values are properly set.");
        }

        logger.info("MinIntensity: {}. | MaxIntensity: {}. | Mintime: {}. | Maxtime: {}.",MINIMUM_PIZAP_DAMAGE, MAXIMUM_PIZAP_DAMAGE, MINIMUM_PIZAP_LENGTH, MAXIMUM_PIZAP_LENGTH);

        if(INVALID_PISHOCK_DATA){
            //Resets to default to avoid any weird shit like divide per 0 exceptions.
            MINIMUM_PIZAP_DAMAGE = 1;
            MAXIMUM_PIZAP_DAMAGE = 100;
            MINIMUM_PIZAP_LENGTH = 1;
            MAXIMUM_PIZAP_LENGTH= 8;
        }

    }

    @Override
    public void receivePostInitialize() {
        //This loads the image used as an icon in the in-game mods menu.
        Texture badgeTexture = TextureLoader.getTexture(imagePath("badge.png"));
        //Set up the mod information displayed in the in-game mods menu.
        //The information used is taken from your pom.xml file.

        //If you want to set up a config panel, that will be done here.
        //The Mod Badges page has a basic example of this, but setting up config is overall a bit complex.
        BaseMod.registerModBadge(badgeTexture, info.Name, GeneralUtils.arrToString(info.Authors), info.Description, null);
    }




    /*----------Localization----------*/

    //This is used to load the appropriate localization files based on language.
    private static String getLangString()
    {
        return Settings.language.name().toLowerCase();
    }
    private static final String defaultLanguage = "eng";

    public static final Map<String, KeywordInfo> keywords = new HashMap<>();

    @Override
    public void receiveEditStrings() {
        /*
            First, load the default localization.
            Then, if the current language is different, attempt to load localization for that language.
            This results in the default localization being used for anything that might be missing.
            The same process is used to load keywords slightly below.
        */
        loadLocalization(defaultLanguage); //no exception catching for default localization; you better have at least one that works.
        if (!defaultLanguage.equals(getLangString())) {
            try {
                loadLocalization(getLangString());
            }
            catch (GdxRuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadLocalization(String lang) {
        //While this does load every type of localization, most of these files are just outlines so that you can see how they're formatted.
        //Feel free to comment out/delete any that you don't end up using.
        BaseMod.loadCustomStringsFile(CardStrings.class,
                localizationPath(lang, "CardStrings.json"));
        BaseMod.loadCustomStringsFile(CharacterStrings.class,
                localizationPath(lang, "CharacterStrings.json"));
        BaseMod.loadCustomStringsFile(EventStrings.class,
                localizationPath(lang, "EventStrings.json"));
        BaseMod.loadCustomStringsFile(OrbStrings.class,
                localizationPath(lang, "OrbStrings.json"));
        BaseMod.loadCustomStringsFile(PotionStrings.class,
                localizationPath(lang, "PotionStrings.json"));
        BaseMod.loadCustomStringsFile(PowerStrings.class,
                localizationPath(lang, "PowerStrings.json"));
        BaseMod.loadCustomStringsFile(RelicStrings.class,
                localizationPath(lang, "RelicStrings.json"));
        BaseMod.loadCustomStringsFile(UIStrings.class,
                localizationPath(lang, "UIStrings.json"));
    }

    @Override
    public void receiveEditKeywords()
    {
        Gson gson = new Gson();
        String json = Gdx.files.internal(localizationPath(defaultLanguage, "Keywords.json")).readString(String.valueOf(StandardCharsets.UTF_8));
        KeywordInfo[] keywords = gson.fromJson(json, KeywordInfo[].class);
        for (KeywordInfo keyword : keywords) {
            keyword.prep();
            registerKeyword(keyword);
        }

        if (!defaultLanguage.equals(getLangString())) {
            try
            {
                json = Gdx.files.internal(localizationPath(getLangString(), "Keywords.json")).readString(String.valueOf(StandardCharsets.UTF_8));
                keywords = gson.fromJson(json, KeywordInfo[].class);
                for (KeywordInfo keyword : keywords) {
                    keyword.prep();
                    registerKeyword(keyword);
                }
            }
            catch (Exception e)
            {
                logger.warn(modID + " does not support " + getLangString() + " keywords.");
            }
        }
    }

    private void registerKeyword(KeywordInfo info) {
        BaseMod.addKeyword(modID.toLowerCase(), info.PROPER_NAME, info.NAMES, info.DESCRIPTION);
        if (!info.ID.isEmpty())
        {
            keywords.put(info.ID, info);
        }
    }

    //These methods are used to generate the correct filepaths to various parts of the resources folder.
    public static String localizationPath(String lang, String file) {
        return resourcesFolder + "/localization/" + lang + "/" + file;
    }

    public static String imagePath(String file) {
        return resourcesFolder + "/images/" + file;
    }
    public static String characterPath(String file) {
        return resourcesFolder + "/images/character/" + file;
    }
    public static String powerPath(String file) {
        return resourcesFolder + "/images/powers/" + file;
    }
    public static String relicPath(String file) {
        return resourcesFolder + "/images/relics/" + file;
    }

    /**
     * Checks the expected resources path based on the package name.
     */
    private static String checkResourcesPath() {
        String name = PiShockMod.class.getName(); //getPackage can be iffy with patching, so class name is used instead.
        int separator = name.indexOf('.');
        if (separator > 0)
            name = name.substring(0, separator);

        FileHandle resources = new LwjglFileHandle(name, Files.FileType.Internal);
        if (resources.child("images").exists() && resources.child("localization").exists()) {
            return name;
        }

        throw new RuntimeException("\n\tFailed to find resources folder; expected it to be named \"" + name + "\"." +
                " Either make sure the folder under resources has the same name as your mod's package, or change the line\n" +
                "\t\"private static final String resourcesFolder = checkResourcesPath();\"\n" +
                "\tat the top of the " + PiShockMod.class.getSimpleName() + " java file.");
    }

    /**
     * This determines the mod's ID based on information stored by ModTheSpire.
     */
    private static void loadModInfo() {
        Optional<ModInfo> infos = Arrays.stream(Loader.MODINFOS).filter((modInfo)->{
            AnnotationDB annotationDB = Patcher.annotationDBMap.get(modInfo.jarURL);
            if (annotationDB == null)
                return false;
            Set<String> initializers = annotationDB.getAnnotationIndex().getOrDefault(SpireInitializer.class.getName(), Collections.emptySet());
            return initializers.contains(PiShockMod.class.getName());
        }).findFirst();
        if (infos.isPresent()) {
            info = infos.get();
            modID = info.ID;
        }
        else {
            throw new RuntimeException("Failed to determine mod info/ID based on initializer.");
        }
    }

    @Override
    public void receiveEditCards() {
        new AutoAdd(modID) //Loads files from this mod
                .packageFilter(BaseCard.class) //In the same package as this class
                .setDefaultSeen(true) //And marks them as seen in the compendium
                .cards(); //Adds the cards
    }

    @Override
    public void receiveEditRelics() {
        new AutoAdd(modID) //Loads files from this mod
                .packageFilter(BaseRelic.class) //In the same package as this class
                .any(BaseRelic.class, (info, relic) -> { //Run this code for any classes that extend this class
                    if (relic.pool != null)
                        BaseMod.addRelicToCustomPool(relic, relic.pool); //Register a custom character specific relic
                    else
                        BaseMod.addRelic(relic, relic.relicType); //Register a shared or base game character specific relic

                    //If the class is annotated with @AutoAdd.Seen, it will be marked as seen, making it visible in the relic library.
                    //If you want all your relics to be visible by default, just remove this if statement.
                    if (info.seen)
                        UnlockTracker.markRelicAsSeen(relic.relicId);
                });
    }


    @Override
    public void receivePostCreateStartingRelics(AbstractPlayer.PlayerClass playerClass, ArrayList<String> relicsToAdd) {
        if(!OLD_SHARECODE_DETECTED) {
            relicsToAdd.add("pishockthespire:PiShockCollar");
            UnlockTracker.markRelicAsSeen("pishockthespire:PiShockCollar");
        }
        else{
            relicsToAdd.add("pishockthespire:WarningErrorRelic");
            UnlockTracker.markRelicAsSeen("pishockthespire:WarningErrorRelic");
        }
    }


    @Override
    public void receivePostDeath() {
        if(!GameOverScreen.isVictory) {
            triggerPiShockCollar(MAXIMUM_PIZAP_DAMAGE, MAXIMUM_PIZAP_LENGTH);
        }
    }

    public static void generateConfigFile(File file){
        try {
            file.createNewFile();
            FileWriter myWriter = new FileWriter(file);

            myWriter.write("# Welcome to the PiShockTheSpire initialization file!\n" +
                            "# Please replace the undefined fields to link your PiShock collar into PiShockTheSpire.\n" +
                            "\n" +
                            "# Username you use to log into PiShock.com. Can be found in the Account section of the website.\n" +
                            "username=undefined\n" +
                            "\n" +
                            "# API Key. You can generate it on PiShock.com, in the Account section of the website.\n" +
                            "apikey=undefined\n" +
                            "\n" +
                            "# Shocker ID. Your Shocker number. You can check it on Pishock.com.\n" +
                            "shockerid=undefined\n" +
                            "\n" +
                            "\n" +
                            "# The following values can be adjusted for a more enjoyable experience.\n" +
                            "# Please ensure the limits set here do not exceed the limitations of your Shocker.\n" +
                            "# Otherwise, you will not be shocked on requests exceeding your own time/intensity limits.\n" +
                            "\n" +
                            "# The minimum intensity for a shock output. It must be bigger (or equal) than 1.\n" +
                            "minPower=10\n" +
                            "\n" +
                            "# The maximum intensity for a shock output. It must be smaller (or equal) than 100, and bigger than minPower.\n" +
                            "maxPower=75\n" +
                            "\n" +
                            "# The minimum amount of seconds a shock output can last. It  must be bigger (or equal) than 1.\n" +
                            "minTime=1\n" +
                            "\n" +
                            "# The maximum amount of seconds a shock output can last.\n" +
                            "maxTime=7"
            );
            myWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


   public static void loadConfigFile(File file){

        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();

            while (line != null) {
                if(!line.isEmpty()){
                    if(line.charAt(0)!= '#' && line.charAt(0)!= '['){
                        String[] splitline = line.split("=");

                        for(String l : splitline) {
                            l = l.replaceAll("\\s+","");
                        }

                        switch (splitline[0]){
                            case "username":
                                USERNAME = splitline[1];
                                break;
                            case "apikey":
                                APIKEY = splitline[1];
                                break;
                            case "shockerid":
                                SHOCKERID = splitline[1];
                                break;
                            case "minPower":
                                MINIMUM_PIZAP_DAMAGE = Integer.parseInt(splitline[1]);
                                break;
                            case "maxPower":
                                MAXIMUM_PIZAP_DAMAGE = Integer.parseInt(splitline[1]);
                                break;
                            case "minTime":
                                MINIMUM_PIZAP_LENGTH = Integer.parseInt(splitline[1]);
                                break;
                            case "maxTime":
                                MAXIMUM_PIZAP_LENGTH = Integer.parseInt(splitline[1]);
                                break;
                            case "sharecode":
                                OLD_SHARECODE_DETECTED = true;
                                logger.info("PiShockConfig contains an invalid field! (Sharecodes have been depreciated).");
                                logger.info("Please check your pishockconfig.txt has been updated with the new PiShock API requirement fields (You will need a ShockedID from now on).");
                                logger.info("TIP: Deleting pishockconfig.txt and regenerating it again by relaunching the game should result in a valid, up to date config file.");
                                break;
                            default:
                                logger.info("PiShockConfig contains an invalid field!");
                                logger.info("TIP: Deleting pishockconfig.txt and regenerating it again by relaunching the game should result in a valid, up to date config file.");
                                break;
                        }
                    }
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
   }

   public static void triggerPiShockCollar(int power, int duration){
        Thread myThread = new triggerPiShockThread(power, duration);
        myThread.start();
   }

   public static class triggerPiShockThread extends Thread {

        private int powerToSend = 1;
        private int durationToSend = 1;

        public triggerPiShockThread(int pwr, int dur){
            powerToSend = pwr;
            durationToSend = dur;
        }


        @Override
        public void run(){

            if(!INVALID_PISHOCK_DATA) {
                try {

                    URL url = new URL("https://api.pishock.com/Shockers/" + SHOCKERID);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");

                    con.setRequestProperty("accept", "*/*");
                    con.setRequestProperty("X-PiShock-Api-Key", APIKEY);
                    con.setRequestProperty("X-PiShock-UserId", USERNAME);
                    con.setRequestProperty("Content-Type", "application/json");

                    con.setDoOutput(true);

                    String jsonInputString = "{\"AgentName\":\"PiShockTheSpire\",\"Operation\": 0,\"Duration\": " + (durationToSend*1000) + ",\"Intensity\": " + powerToSend + ",\"IntensityAsPercentage\": true}";

                    logger.info(jsonInputString);

                    try (OutputStream os = con.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int responseCode = con.getResponseCode();
                    String responseData = con.getResponseMessage();
                    logger.info("Shock request sent - PiShock API response code: {} -> {}", responseCode, responseData);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
   }

}
