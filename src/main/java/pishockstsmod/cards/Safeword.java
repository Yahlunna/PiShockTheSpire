package pishockstsmod.cards;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.unique.BlockPerNonAttackAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import pishockstsmod.powers.PiShockAlter;
import pishockstsmod.util.CardStats;

public class Safeword extends BaseCard {

    public static final String ID = makeID("Safeword");

    private static final CardStats info = new CardStats(
            CardColor.COLORLESS,
            CardType.SKILL,
            CardRarity.COMMON,
            CardTarget.SELF,
            0
    );

    public Safeword() {
        super(ID, info); //Pass the required information to the BaseCard constructor.

        setSelfRetain(true);
        setMagic(0,0);
        setExhaust(true, false);
    }

    @Override
    public void use(AbstractPlayer abstractPlayer, AbstractMonster abstractMonster) {
        addToTop(new RemoveSpecificPowerAction(abstractPlayer, abstractPlayer, "pishockthespire:PiShockAlter"));
        addToBot(new ApplyPowerAction(abstractPlayer, abstractPlayer, new PiShockAlter(abstractPlayer, magicNumber)));
    }

}
