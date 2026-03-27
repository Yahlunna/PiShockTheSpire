package pishockstsmod.relics;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import pishockstsmod.cards.Safeword;

import java.util.Objects;

import static pishockstsmod.PiShockMod.*;

public class PiShockCollar extends BaseRelic {
    private static final String NAME = "PiShockCollar"; //The name will be used for determining the image file as well as the ID.
    public static final String ID = makeID(NAME); //This adds the mod's prefix to the relic ID, resulting in modID:MyRelic
    private static final RelicTier RARITY = RelicTier.STARTER; //The relic's rarity.
    private static final LandingSound SOUND = LandingSound.CLINK; //The sound played when the relic is clicked.

    private int hitsTaken = 0;
    private int damageTaken = 0;
    private int piShockModifier = 1;
    private boolean isInCombat = false;

    public PiShockCollar() {
        super(ID, NAME, RARITY, SOUND);
    }


    // At the beginning of combat, add a safeword to
    @Override
    public void atBattleStart() {
        isInCombat= true;
        resetDamageTaken();
        addToTop(new MakeTempCardInHandAction(new Safeword(), 1));
    }

    public void onVictory() {
        resetDamageTaken();
        isInCombat= false;
    }


    @Override
    public String getUpdatedDescription() {
        return String.format(DESCRIPTIONS[0]);
    }

    @Override
    public void onLoseHp(int damageAmount) {
        //Should check if its during combat. (maybe take damage during events too???)
        //Outside a combat room, just trigger pishocker, then clean.
        hitsTaken++;
        damageTaken += damageAmount;
        //During combat
        if(!isInCombat) {
            logger.info("Damage taken outside combat!");
            hitsTaken = Math.min(3, MAXIMUM_PIZAP_DAMAGE);
            triggerPiShocker();
            resetDamageTaken();
        }
    }

    @Override
    public void atTurnStart() {
        damageTaken *= piShockModifier;
        if(damageTaken > 0){
            triggerPiShocker();
        }
        resetDamageTaken();
    }

    @Override
    public void onPlayerEndTurn() {
        hitsTaken = 0;
        damageTaken = 0;
    }

    @Override
    public void onUseCard(AbstractCard targetCard, UseCardAction useCardAction) {
        if(Objects.equals(targetCard.cardID, "pishockthespire:Safeword")){
            piShockModifier = targetCard.magicNumber;
        }
    }

    private void resetDamageTaken(){
        hitsTaken = 0;
        damageTaken = 0;
        piShockModifier = 1;
    }

    private int calculateZapOutput(){
        float zapOutput = ((float) damageTaken / (float) AbstractDungeon.player.maxHealth) * 100;
        if (zapOutput > 100 ) {zapOutput = 100;} // Max discharge tops at 100% because duh. - this can be triggered with stuff like lizzard tail adn that kind of shit-
        if (zapOutput < 1) {zapOutput = 1;} // For small hits when max HP is over 100.

        int damageRange= MAXIMUM_PIZAP_DAMAGE - MINIMUM_PIZAP_DAMAGE;

        if(damageRange != 99) {
            zapOutput = (zapOutput / 100) * damageRange;
            zapOutput += MINIMUM_PIZAP_DAMAGE;

            //Redundant, but better safe than sorry.
            if (zapOutput > MAXIMUM_PIZAP_DAMAGE) {
                zapOutput = MAXIMUM_PIZAP_DAMAGE;
            }
            if (zapOutput < MINIMUM_PIZAP_DAMAGE) {
                zapOutput = MINIMUM_PIZAP_DAMAGE;
            }
        }

        return Math.round(zapOutput);
    }

    private int calculateZapDuration(){
        if(hitsTaken > MAXIMUM_PIZAP_LENGTH){
            return MAXIMUM_PIZAP_LENGTH;
        }
        else if(hitsTaken < MINIMUM_PIZAP_LENGTH){
            return MINIMUM_PIZAP_LENGTH;
        }
        else
            return hitsTaken;
    }


    public void triggerPiShocker(){
        this.flash();
        triggerPiShockCollar(calculateZapOutput(), calculateZapDuration());
        logger.info("Zapped! PiShock triggered with a power of {}% for a length of {} seconds.", calculateZapOutput(), calculateZapDuration());
    }


}