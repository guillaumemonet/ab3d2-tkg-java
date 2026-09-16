package ab3d2.data;

import ab3d2.Mem;

/**
 * Traduction littérale de ab3d2_source/data/text_data.s
 *
 * Les chaînes sont copiées verbatim (longueurs et espaces de fin compris) :
 * options 40 colonnes, messages de victoire 80 octets, texte de fin en
 * enregistrements de 82 octets (0, drawFlag, 80 caractères).
 */
public final class TextData {

    public static final int Game_SoundOptionsText_vb;
    public static final int Game_LightingOptionsText_vb;
    public static final int Game_TwoPlayerVictoryMessages_vb;
    public static final int Game_DrawHighQualityText_vb;
    public static final int Game_DrawLowQualityText_vb;
    public static final int Game_CantCollectItemText_vb;
    public static final int Game_InputKeyboard_vb;
    public static final int Game_InputJoystick_vb;
    public static final int Game_InputMouse_vb;
    public static final int Game_InputMouseInv_vb;
    public static final int Game_SinglePlayerVictoryText_vb;
    public static final int ENDENDGAMETEXT;

    static {
        Mem.align(4);

        //          "1234567890123456789012345678901234567890"
        Game_SoundOptionsText_vb = Mem.dcStr("Audio: Four Channel Mono                ");
        Mem.dcStr("Audio: Four Channel Stereo              ");
        Mem.dcStr("Audio: Eight Channel Mono               ");
        Mem.dcStr("Audio: Eight Channel Stereo             ");

        Game_LightingOptionsText_vb = Mem.dcStr("Lighting: Moving Lightsources Disabled  ");
        Mem.dcStr("Lighting: Moving Lightsources Enabled   ");

        Game_TwoPlayerVictoryMessages_vb = Mem.dcStr("Enemy Player Vanquished!                ");
        Mem.dcStr("                                        ");

        Mem.dcStr("Oooh, that one must have hurt!          ");
        Mem.dcStr("                                        ");

        Mem.dcStr("Opponent IS toast!                      ");
        Mem.dcStr("                                        ");

        Mem.dcStr("Does it hurt? DOES it? DOES IT?!?       ");
        Mem.dcStr("                                        ");

        Mem.dcStr("Gosh, I'm dreadfully sorry, old chap; didn't see you there!                     ");

        Mem.dcStr("Now go away before I taunt you a second time.                                   ");

        Mem.dcStr("Eh, sorry about that there mate, didn't know it was loaded, know worra mean?    ");

        Mem.dcStr("Stand and deliver, your money or...  oh. Never mind.                            ");

        Mem.dcStr("Thank you for your kind interest, I look forward to your custom in future lives.");

        Game_DrawHighQualityText_vb = Mem.dcStr("Renderer: High Quality");
        Mem.dcB(0);

        Game_DrawLowQualityText_vb = Mem.dcStr("Renderer: Reduced Quality");
        Mem.dcB(0);

        Game_CantCollectItemText_vb = Mem.dcStr("I can't carry any more of these just now.");
        Mem.dcB(0);

        Game_InputKeyboard_vb = Mem.dcStr("Input: Keyboard Only");
        Mem.dcB(0);

        Game_InputJoystick_vb = Mem.dcStr("Input: Joystick");
        Mem.dcB(0);

        Game_InputMouse_vb = Mem.dcStr("Input: Mouse and Keyboard");
        Mem.dcB(0);

        Game_InputMouseInv_vb = Mem.dcStr("Input: Mouse (Inverted) and Keyboard");
        Mem.dcB(0);

        Mem.align(4);
        Game_SinglePlayerVictoryText_vb = Mem.allocTop();
        //   "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
        line(0, "                                                                                ");
        line(1, "As the beast and its four servants die, a breathless silence falls, broken      ");
        line(1, "only by the hammering of my own heart in my chest.                              ");
        line(1, "I run to the now open exit, and out into the maze of corridors through which I  ");
        line(1, "came. I encounter many, many aliens, lying twitching on the ground, or utterly  ");
        line(1, "still with glazed eyes and green froth drying on their lips. Many seem to have  ");
        line(1, "turned their weapons on themselves, unable to bear either the pain or the       ");
        line(1, "sudden silence in their minds.                                                  ");
        line(0, "                                                                                ");
        line(1, "It takes me several hours to locate a working teleport to take me back aboard   ");
        line(1, "the orbiting alien ship. The scene there is the same; hordes of aliens, either  ");
        line(1, "dead or catatonic, I cannot tell.                                               ");
        line(1, "I walk slowly, exhausted, back to the INDOMITABLE, averting my eyes from the    ");
        line(1, "pitiful scenes around me. I know that my work is not finished yet.              ");
        line(1, "Once aboard, I make my way to the bridge. I manage to restart the main power    ");
        line(1, "generators and get basic navigation back on-line. Working from the memories     ");
        line(1, "implanted by the dying marine, I painstakingly program the computer to deal     ");
        line(1, "the killing blow to the enemy. At last the task is finished. The ship hums into ");
        line(1, "life, accellerating slowly out of orbit, towing the massive alien craft and     ");
        line(1, "its mindless cargo behind it.                                                   ");
        line(1, "As the image of the alien sun grows in the viewscreen, I think about what I     ");
        line(1, "have seen. Fragments of technology, stolen from civilisations - how long ago?   ");
        line(1, "How long since they were exterminated by these parasites? And how many more     ");
        line(1, "if they are allowed to continue?                                                ");
        line(1, "The sun looms hideously large before me, seeming at the last moment to slip to  ");
        line(1, "one side as the cruiser slingshots itself through the immense gravity well,     ");
        line(1, "its speed doubling and doubling again. The ship shudders and groans as the      ");
        line(1, "ponderous mass of the alien ship tries to tear itself free. I feel a distant    ");
        line(1, "twinge of curiosity as to whether it will succeed.                              ");
        line(1, "The navigation computer chatters quietly to itself as it makes tiny             ");
        line(1, "course corrections, bringing the payload to bear on its target. I only sit,     ");
        line(1, "watching blankly as we hurtle back towards the planet. The navicom beeps        ");
        line(1, "quietly to signal the blowing of the explosive bolts holding the docking ring.  ");
        line(1, "So great is our speed that the alien ship does not receed, but simply vanishes  ");
        line(1, "from sight, tracked only on the readouts of the computers in front of me.       ");
        line(1, "One readout in particular occupies my attention. Red numbers spin towards zero  ");
        line(1, "as my invisible agent of destruction spins towards oblivion. Three digits, now  ");
        line(1, "two, and now only one. I shift my attention to the image of the alien world,    ");
        line(1, "receeding behind my ship.                                                       ");
        line(0, "                                                                                ");
        line(1, "Three...                                                                        ");
        line(0, "                                                                                ");
        line(1, "Two...                                                                          ");
        line(0, "                                                                                ");
        line(1, "One...                                                                          ");
        line(0, "                                                                                ");
        line(1, "Zero.                                                                           ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "Travelling at nearly a quarter of the speed of light, the alien ship smashed    ");
        line(1, "into the planet, flashing past the useless orbital defences which should have   ");
        line(1, "neutralized it millions of miles earlier. It passed through the twenty miles    ");
        line(1, "of atmosphere in a little more than one ten-thousandth of a second. The air     ");
        line(1, "directly beneath had no time to be pushed out of the way, and in another        ");
        line(1, "tenth of a second it was a molecule-thick layer a thousand miles below the      ");
        line(1, "planet's surface.                                                               ");
        line(1, "Such was the heat and pressure caused by the impact, that part of the molten    ");
        line(1, "core of the planet underwent nuclear fusion, vapourising thousands of billions  ");
        line(1, "of tonnes of surrounding material. This expanding superhot plasma cloud forced  ");
        line(1, "its way up through the mantle and crust, fracturing the surface of the planet,  ");
        line(1, "blowing continent-sized chunks into space and heating the tortured atmosphere   ");
        line(1, "to ignition point. Within two minutes of impact, the doomed planet was a        ");
        line(1, "misshapen, incandescent ball, with burning fragments spinning deceptively       ");
        line(1, "slowly in their brief orbits before re-impacting with fantastic, majestic       ");
        line(1, "force.                                                                          ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "In a matter of weeks, the small amount of matter which had undergone fusion     ");
        line(1, "burned itself out, but the planet still glowed sullenly from a million cracks   ");
        line(1, "and holes in the crust, as it would continue to do for millions of years to     ");
        line(1, "come. The world was barren and dead, and the creatures who once roamed its      ");
        line(1, "surface no more than a memory in the mind of one man, sleeping dreamlessly      ");
        line(1, "as the invisible speck of his ship sped silently on, towards home.              ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "ALIEN BREED 3D II                                                               ");
        line(1, "THE KILLING GROUNDS                                                             ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "A Team 17 Game                                                                  ");
        line(0, "                                                                                ");
        line(1, "Produced in association with OCEAN Software                                     ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "Game Design, Game Code, Editor Code and In-Game Text                            ");
        line(0, "                                                                                ");
        line(1, "Andrew Clitheroe                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "Graphics                                                                        ");
        line(0, "                                                                                ");
        line(1, "Michael Green                                                                   ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "3D Object Designs, 3D Editors, Serial and OS code                               ");
        line(0, "                                                                                ");
        line(1, "Charles Blessing                                                                ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "Music                                                                           ");
        line(0, "                                                                                ");
        line(1, "Ben Chanter                                                                     ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "Project Manager                                                                 ");
        line(0, "                                                                                ");
        line(1, "Phil Quirke-Webster                                                             ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "Playtesting                                                                     ");
        line(0, "                                                                                ");
        line(1, "Phil and the Wolves                                                             ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        line(1, "Additional Graphics                                                             ");
        line(0, "                                                                                ");
        line(1, "Pete Lyons                                                                      ");
        line(0, "                                                                                ");
        line(0, "                                                                                ");
        ENDENDGAMETEXT = Mem.allocTop();
    }

    /** dc.b 0,flag,"80 caractères" */
    private static void line(int flag, String text) {
        Mem.dcB(0, flag);
        Mem.dcStr(text);
    }

    private TextData() {
    }
}
