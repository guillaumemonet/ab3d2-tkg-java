package ab3d2.modules;

import ab3d2.ControlloopData;
import ab3d2.HiresData;
import ab3d2.Mem;
import ab3d2.c.ZoneEdgePvs;
import ab3d2.data.LevelData;
import ab3d2.host.ExecLib;

import static ab3d2.Defs.GLFT_FloorFilename_l;
import static ab3d2.Defs.GLFT_LevelMusic_l;
import static ab3d2.Defs.GLFT_ObjGfxNames_l;
import static ab3d2.Defs.GLFT_SFXFilenames_l;
import static ab3d2.Defs.GLFT_TextureFilename_l;
import static ab3d2.Defs.GLFT_VectorNames_l;
import static ab3d2.Defs.GLFT_WallGFXNames_l;
import static ab3d2.Defs.NUM_WALL_TEXTURES;
import static ab3d2.M68k.setb;
import static ab3d2.M68k.setw;
import static ab3d2.bss.DrawBss.DRAW_MAX_OBJECTS;
import static ab3d2.bss.DrawBss.DRAW_MAX_POLY_OBJECTS;
import static ab3d2.bss.DrawBss.Draw_BackdropImagePtr_l;
import static ab3d2.bss.DrawBss.Draw_FloorTexturesPtr_l;
import static ab3d2.bss.DrawBss.Draw_GlobalFloorTexturesPtr_l;
import static ab3d2.bss.DrawBss.Draw_GlobalWallTexturePtrs_vl;
import static ab3d2.bss.DrawBss.Draw_LevelFloorTexturesPtr_l;
import static ab3d2.bss.DrawBss.Draw_LevelWallTexturePtrs_vl;
import static ab3d2.bss.DrawBss.Draw_ObjectPtrs_vl;
import static ab3d2.bss.DrawBss.Draw_PolyObjects_vl;
import static ab3d2.bss.DrawBss.Draw_TextureMapsPtr_l;
import static ab3d2.bss.DrawBss.Draw_TexturePalettePtr_l;
import static ab3d2.bss.DrawBss.Draw_WallTexturePtrs_vl;
import static ab3d2.bss.IoBss.IO_MemType_l;
import static ab3d2.bss.IoBss.io_Buffer_vb;
import static ab3d2.bss.IoBss.io_FileExtPointer_l;
import static ab3d2.bss.IoBss.io_ObjectName_vb;
import static ab3d2.bss.LevelBss.Lvl_ClipsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_DataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ErrataPtr_l;
import static ab3d2.bss.LevelBss.Lvl_GraphicsPtr_l;
import static ab3d2.bss.LevelBss.Lvl_ModPropertiesPtr_l;
import static ab3d2.bss.TablesBss.Aud_SampleList_vl;
import static ab3d2.modules.FileIo.MEMF_ANY;

/**
 * Traduction littérale de ab3d2_source/modules/res.s
 *
 * "Definitions specific to the management of resource data (graphics, sounds,
 *  models, level data etc.) — Mostly refactored from newloadfromdisk.s and
 *  wallchunk.s"
 */
public final class Res {

    /** XXX (original): Shouldn't this be NUM_SFX? But doesn't work with karlos-tkg */
    public static final int RES_NUM_SFX = 59;

    // .bin2hex (données embarquées dans Res_LoadLevelData)
    private static final int bin2hex;

    static {
        bin2hex = Mem.dcStr("0123456789ABCDEF");
        Mem.align(4);
    }

    private Res() {
    }

    // *****************************************************************************
    // * Objects (bitmap and vector)
    // *****************************************************************************

    /** Res_LoadObjects */
    public static void Res_LoadObjects() {
        int a0 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a0
        a0 = a0 + GLFT_ObjGfxNames_l;                  // lea GLFT_ObjGfxNames_l(a0),a0
        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        int a1 = Draw_ObjectPtrs_vl;                   // move.l #Draw_ObjectPtrs_vl,a1

        do { // .load_object_loop:
            int a4 = a0;                               // move.l a0,a4
            int a3 = io_ObjectName_vb;                 // move.l #io_ObjectName_vb,a3

            int d0;
            while (true) { // .fill_name:
                d0 = Mem.ub(a4); a4 += 1;              // move.b (a4)+,d0
                if ((byte) d0 == 0) {                  // beq.s .done_name
                    break;
                }
                Mem.wb(a3, d0); a3 += 1;               // move.b d0,(a3)+
            }                                          // bra.s .fill_name

            // .done_name:
            int saved_a0 = a0;                         // move.l a0,-(a7)
            Mem.wl(io_FileExtPointer_l, a3);           // move.l a3,io_FileExtPointer_l
            Mem.wb(a3, '.'); a3 += 1;                  // move.b #'.',(a3)+
            Mem.wb(a3, 'W'); a3 += 1;                  // move.b #'W',(a3)+
            Mem.wb(a3, 'A'); a3 += 1;                  // move.b #'A',(a3)+
            Mem.wb(a3, 'D'); a3 += 1;                  // move.b #'D',(a3)+
            Mem.wb(a3, 0); a3 += 1;                    // move.b #0,(a3)+
            FileIo.IO_QueueFile(io_ObjectName_vb, a1, 0); // move.l #io_ObjectName_vb,a0 ; move.l a1,d0 ; moveq #0,d1 ; bsr IO_QueueFile

            a3 = Mem.l(io_FileExtPointer_l);           // move.l io_FileExtPointer_l,a3
            Mem.wb(a3, '.'); a3 += 1;                  // move.b #'.',(a3)+
            Mem.wb(a3, 'P'); a3 += 1;                  // move.b #'P',(a3)+
            Mem.wb(a3, 'T'); a3 += 1;                  // move.b #'T',(a3)+
            Mem.wb(a3, 'R'); a3 += 1;                  // move.b #'R',(a3)+
            Mem.wb(a3, 0); a3 += 1;                    // move.b #0,(a3)+
            FileIo.IO_QueueFile(io_ObjectName_vb, a1 + 4, 0); // move.l a1,d0 ; add.l #4,d0 ; bsr IO_QueueFile

            a3 = Mem.l(io_FileExtPointer_l);           // move.l io_FileExtPointer_l,a3
            Mem.wb(a3, '.'); a3 += 1;                  // move.b #'.',(a3)+
            Mem.wb(a3, '2'); a3 += 1;                  // move.b #'2',(a3)+
            Mem.wb(a3, '5'); a3 += 1;                  // move.b #'5',(a3)+
            Mem.wb(a3, '6'); a3 += 1;                  // move.b #'6',(a3)+
            Mem.wb(a3, 'P'); a3 += 1;                  // move.b #'P',(a3)+
            Mem.wb(a3, 'A'); a3 += 1;                  // move.b #'A',(a3)+
            Mem.wb(a3, 'L'); a3 += 1;                  // move.b #'L',(a3)+
            Mem.wb(a3, 0); a3 += 1;                    // move.b #0,(a3)+
            FileIo.IO_QueueFile(io_ObjectName_vb, a1 + 12, 0); // move.l a1,d0 ; add.l #12,d0 ; bsr IO_QueueFile

            a0 = saved_a0;                             // move.l (a7)+,a0
            a0 += 64;                                  // add.l #64,a0
            a1 += 16;                                  // add.l #16,a1
        } while (Mem.b(a0) != 0);                      // tst.b (a0) ; bne .load_object_loop

        int a2 = Draw_PolyObjects_vl;                  // move.l #Draw_PolyObjects_vl,a2
        a0 = Mem.l(HiresData.GLF_DatabasePtr_l);       // move.l GLF_DatabasePtr_l,a0
        a0 += GLFT_VectorNames_l;                      // add.l #GLFT_VectorNames_l,a0

        while (Mem.b(a0) != 0) { // .load_vector_loop: tst.b (a0) ; beq.s .end_load_vectors
            FileIo.IO_QueueFile(a0, a2, 0);            // move.l a2,d0 ; moveq #0,d1 ; jsr IO_QueueFile
            a2 += 4;                                   // addq #4,a2
            a0 += 64;                                  // adda.w #64,a0
        }                                              // bra.s .load_vector_loop
        // .end_load_vectors: rts
    }

    /** RES_FREEPTR var : a1 = (var) ; clr.l var ; FreeVec */
    private static void RES_FREEPTR(int varAddr) {
        int a1 = Mem.l(varAddr);                       // move.l \1,a1
        Mem.wl(varAddr, 0);                            // clr.l \1
        ExecLib.FreeVec(a1);                           // CALLEXEC FreeVec
    }

    /** Res_FreeObjects */
    public static void Res_FreeObjects() {
        RES_FREEPTR(HiresData.GLF_DatabasePtr_l);      // RES_FREEPTR GLF_DatabasePtr_l
        RES_FREEPTR(ControlloopData.Lvl_IntroTextPtr_l); // RES_FREEPTR Lvl_IntroTextPtr_l
        RES_FREEPTR(Draw_BackdropImagePtr_l);          // RES_FREEPTR Draw_BackdropImagePtr_l
        int d2 = DRAW_MAX_OBJECTS * 4 - 1;             // move.w #DRAW_MAX_OBJECTS*4-1,d2
        int a2 = Draw_ObjectPtrs_vl;                   // lea Draw_ObjectPtrs_vl,a2
        res_FreeList(d2, a2);                          // bsr res_FreeList
        d2 = DRAW_MAX_POLY_OBJECTS - 1;                // moveq #DRAW_MAX_POLY_OBJECTS-1,d2
        a2 = Draw_PolyObjects_vl;                      // lea Draw_PolyObjects_vl,a2
        res_FreeList(d2, a2);                          // fall through
    }

    /** res_FreeList — d2 = number of pointers-1, a2 = list */
    private static void res_FreeList(int d2, int a2) {
        do { // res_FreeList:
            RES_FREEPTR(a2);                           // RES_FREEPTR (a2)
            a2 += 4;                                   // add.w #4,a2
            d2 = setw(d2, d2 - 1);                     // dbf d2,res_FreeList
        } while ((short) d2 != -1);
        // rts
    }

    // *****************************************************************************
    // * Sound Effects
    // *****************************************************************************

    /** Res_LoadSoundFx */
    public static void Res_LoadSoundFx() {
        int a0 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a0
        a0 = a0 + GLFT_SFXFilenames_l;                 // lea GLFT_SFXFilenames_l(a0),a0
        int a1 = Aud_SampleList_vl;                    // move.l #Aud_SampleList_vl,a1
        int d7 = RES_NUM_SFX - 1;                      // move.w #RES_NUM_SFX-1,d7
        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l

        do { // .load_sound_loop:
            if (Mem.b(a0) != 0) {                      // tst.b (a0) ; beq.s .skip
                int d0 = a1;                           // move.l a1,d0
                int d1 = d0;                           // move.l d0,d1
                d1 += 4;                               // add.l #4,d1
                FileIo.IO_QueueFile(a0, d0, d1);       // jsr IO_QueueFile
            }
            // .skip:
            a1 += 8;                                   // addq #8,a1
            a0 += 64;                                  // adda.w #64,a0
            d7 = setw(d7, d7 - 1);                     // dbra d7,.load_sound_loop
        } while ((short) d7 != -1);
        // rts
    }

    /**
     * Res_PatchSoundFx — transform the list of {{startaddress, length},...}
     * into {{startaddress, endaddress},...}
     */
    public static void Res_PatchSoundFx() {
        int d7 = RES_NUM_SFX - 1;                      // move.w #RES_NUM_SFX-1,d7
        int a1 = Aud_SampleList_vl;                    // move.l #Aud_SampleList_vl,a1

        do { // .patch_loop:
            int d0 = Mem.l(a1); a1 += 4;               // move.l (a1)+,d0
            Mem.wl(a1, Mem.l(a1) + d0); a1 += 4;       // add.l d0,(a1)+
            d7 = setw(d7, d7 - 1);                     // dbra d7,.patch_loop
        } while ((short) d7 != -1);
        // rts
    }

    /** Res_FreeSoundFx */
    public static void Res_FreeSoundFx() {
        int a2 = Aud_SampleList_vl;                    // move.l #Aud_SampleList_vl,a2
        int d2 = RES_NUM_SFX - 1;                      // move.w #RES_NUM_SFX-1,d2
        do { // .relmem:
            int a1 = Mem.l(a2);                        // move.l (a2),a1
            Mem.wl(a2, 0);                             // clr.l (a2)
            ExecLib.FreeVec(a1);                       // CALLEXEC FreeVec
            a2 += 8;                                   // addq.w #8,a2
            d2 = setw(d2, d2 - 1);                     // dbf d2,.relmem
        } while ((short) d2 != -1);
        // rts
    }

    // *****************************************************************************
    // * Floor/Ceiling and model Textures
    // *****************************************************************************

    /** Res_LoadFloorsAndTextures */
    public static void Res_LoadFloorsAndTextures() {
        int a0 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a0
        a0 += GLFT_FloorFilename_l;                    // add.l #GLFT_FloorFilename_l,a0
        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        FileIo.IO_QueueFile(a0, Draw_GlobalFloorTexturesPtr_l, 0); // move.l #...,d0 ; move.l #0,d1 ; jsr IO_QueueFile

        a0 = Mem.l(HiresData.GLF_DatabasePtr_l);       // move.l GLF_DatabasePtr_l,a0
        a0 += GLFT_TextureFilename_l;                  // add.l #GLFT_TextureFilename_l,a0
        int a1 = io_Buffer_vb;                         // move.l #io_Buffer_vb,a1

        while (true) { // .copy_loop:
            int b = Mem.ub(a0); a0 += 1;               // move.b (a0)+,(a1)+
            Mem.wb(a1, b); a1 += 1;
            if (b == 0) {                              // beq.s .copied
                break;
            }
        }                                              // bra.s .copy_loop

        // .copied:
        a1 -= 1;                                       // subq #1,a1
        Mem.wl(io_FileExtPointer_l, a1);               // move.l a1,io_FileExtPointer_l
        FileIo.IO_QueueFile(io_Buffer_vb, Draw_TextureMapsPtr_l, 0); // move.l #io_Buffer_vb,a0 ; ... ; jsr IO_QueueFile

        a1 = Mem.l(io_FileExtPointer_l);               // move.l io_FileExtPointer_l,a1
        Mem.wl(a1, 0x2E70616C);                        // move.l #".pal",(a1)
        FileIo.IO_QueueFile(io_Buffer_vb, Draw_TexturePalettePtr_l, 0); // jsr IO_QueueFile
        // rts
    }

    /** Res_FreeFloorsAndTextures */
    public static void Res_FreeFloorsAndTextures() {
        RES_FREEPTR(Draw_GlobalFloorTexturesPtr_l);    // RES_FREEPTR Draw_GlobalFloorTexturesPtr_l
        RES_FREEPTR(Draw_TextureMapsPtr_l);            // RES_FREEPTR Draw_TextureMapsPtr_l
        RES_FREEPTR(Draw_TexturePalettePtr_l);         // RES_FREEPTR Draw_TexturePalettePtr_l
        // rts
    }

    // *****************************************************************************
    // * Wall Textures
    // *****************************************************************************

    /** Res_LoadWallTextures */
    public static void Res_LoadWallTextures() {
        // New loading system: send each filename to a 'server' along with addresses
        // for the return values (pos,len), then call FLUSHQUEUE.

        int a0 = Draw_GlobalWallTexturePtrs_vl;        // move.l #Draw_GlobalWallTexturePtrs_vl,a0
        int d7 = NUM_WALL_TEXTURES - 1;                // moveq #NUM_WALL_TEXTURES-1,d7

        do { // .empty_walls:
            Mem.wl(a0, 0); a0 += 4;                    // move.l #0,(a0)+
            d7 = setw(d7, d7 - 1);                     // dbra d7,.empty_walls
        } while ((short) d7 != -1);

        int a4 = Draw_GlobalWallTexturePtrs_vl;        // move.l #Draw_GlobalWallTexturePtrs_vl,a4
        int a3 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a3
        a3 += GLFT_WallGFXNames_l;                     // add.l #GLFT_WallGFXNames_l,a3
        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        d7 = NUM_WALL_TEXTURES - 1;                    // move.w #NUM_WALL_TEXTURES-1,d7

        do { // .load_loop:
            int d0 = Mem.l(a3);                        // move.l (a3),d0
            if (d0 == 0) {                             // beq .done ; XXX maybe just skip this entry?
                return;                                // .done: rts
            }
            FileIo.IO_QueueFile(a3, a4, 0);            // move.l a3,a0 ; move.l a4,d0 ; move.l #0,d1 ; jsr IO_QueueFile
            a4 += 4;                                   // addq #4,a4
            a3 += 64;                                  // adda.w #64,a3
            d7 = setw(d7, d7 - 1);                     // dbf d7,.load_loop
        } while ((short) d7 != -1);
        // .done: rts
    }

    /** Res_FreeWallTextures */
    public static void Res_FreeWallTextures() {
        int a2 = Draw_GlobalWallTexturePtrs_vl;        // lea Draw_GlobalWallTexturePtrs_vl,a2
        int d2 = NUM_WALL_TEXTURES - 1;                // moveq #NUM_WALL_TEXTURES-1,d2
        res_FreeList(d2, a2);                          // bra res_FreeList
    }

    // *****************************************************************************
    // * Level Data
    // *****************************************************************************

    /** Res_LoadLevelData */
    public static void Res_LoadLevelData() {
        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        long r = FileIo.IO_LoadFile(LevelData.Lvl_MapFilename_vb); // move.l #Lvl_MapFilename_vb,a0 ; jsr IO_LoadFile
        Mem.wl(HiresData.Lvl_WalkLinksPtr_l, FileIo.addr(r)); // move.l d0,Lvl_WalkLinksPtr_l

        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        r = FileIo.IO_LoadFile(LevelData.Lvl_FlyMapFilename_vb); // jsr IO_LoadFile
        Mem.wl(HiresData.Lvl_FlyLinksPtr_l, FileIo.addr(r)); // move.l d0,Lvl_FlyLinksPtr_l

        int d1 = 0;                                    // moveq #0,d1
        d1 = setb(d1, Mem.ub(LevelData.Lvl_BinFilenameX_vb)); // move.b Lvl_BinFilenameX_vb,d1
        d1 = setb(d1, d1 - 'a');                       // sub.b #'a',d1
        d1 = setw(d1, d1 << 6);                        // lsl.w #6,d1
        int a0 = Mem.l(HiresData.GLF_DatabasePtr_l);   // move.l GLF_DatabasePtr_l,a0
        a0 = a0 + GLFT_LevelMusic_l;                   // lea GLFT_LevelMusic_l(a0),a0
        // NB (original) : d1 = niveau*64 est calculé mais jamais ajouté à a0 —
        // le nom de musique chargé est toujours celui du premier slot.

        Mem.wl(IO_MemType_l, ExecLib.MEMF_CHIP);       // move.l #MEMF_CHIP,IO_MemType_l
        r = FileIo.IO_LoadFile(a0);                    // jsr IO_LoadFile
        Mem.wl(HiresData.Lvl_MusicPtr_l, FileIo.addr(r)); // move.l d0,Lvl_MusicPtr_l

        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        r = FileIo.IO_LoadFile(LevelData.Lvl_BinFilename_vb); // jsr IO_LoadFile
        Mem.wl(Lvl_DataPtr_l, FileIo.addr(r));         // move.l d0,Lvl_DataPtr_l

        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        r = FileIo.IO_LoadFile(LevelData.Lvl_GfxFilename_vb); // jsr IO_LoadFile
        Mem.wl(Lvl_GraphicsPtr_l, FileIo.addr(r));     // move.l d0,Lvl_GraphicsPtr_l

        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        r = FileIo.IO_LoadFile(LevelData.Lvl_ClipsFilename_vb); // jsr IO_LoadFile
        Mem.wl(Lvl_ClipsPtr_l, FileIo.addr(r));        // move.l d0,Lvl_ClipsPtr_l

        // Load the (optional) floor level graphics
        // First, ensure the globals are active
        Mem.wl(Draw_FloorTexturesPtr_l, Mem.l(Draw_GlobalFloorTexturesPtr_l)); // move.l Draw_Global...,Draw_FloorTexturesPtr_l

        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        r = FileIo.IO_LoadFileOptional(LevelData.Lvl_FloorFilename_vb); // jsr IO_LoadFileOptional

        Mem.wl(Draw_LevelFloorTexturesPtr_l, FileIo.addr(r)); // move.l d0,Draw_LevelFloorTexturesPtr_l
        if (FileIo.addr(r) != 0) {                     // beq.s .done_floor_override
            // Override
            Mem.wl(Draw_FloorTexturesPtr_l, Mem.l(Draw_LevelFloorTexturesPtr_l)); // move.l ...,Draw_FloorTexturesPtr_l
        }

        // .done_floor_override:
        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        r = FileIo.IO_LoadFileOptional(LevelData.Lvl_ModPropsFilename_vb); // jsr IO_LoadFileOptional
        Mem.wl(Lvl_ModPropertiesPtr_l, FileIo.addr(r)); // move.l d0,Lvl_ModPropertiesPtr_l

        Mem.wl(IO_MemType_l, MEMF_ANY);                // move.l #MEMF_ANY,IO_MemType_l
        r = FileIo.IO_LoadFileOptional(LevelData.Lvl_ErrataFilename_vb); // jsr IO_LoadFileOptional
        Mem.wl(Lvl_ErrataPtr_l, FileIo.addr(r));       // move.l d0,Lvl_ErrataPtr_l

        Level.Lvl_InitLevelMods();                     // jsr Lvl_InitLevelMods

        // .done_level_properties:
        // movem.l d2/a2/a3/a4/a5,-(sp)
        int a2 = Draw_GlobalWallTexturePtrs_vl;        // move.l #Draw_GlobalWallTexturePtrs_vl,a2
        int a3 = Draw_LevelWallTexturePtrs_vl;         // move.l #Draw_LevelWallTexturePtrs_vl,a3
        int a4 = Draw_WallTexturePtrs_vl;              // move.l #Draw_WallTexturePtrs_vl,a4
        int d2 = NUM_WALL_TEXTURES - 1;                // moveq #NUM_WALL_TEXTURES-1,d2
        int a5 = bin2hex;                              // lea .bin2hex,a5

        do { // .do_wall:
            // First, put the default wall into Draw_WallTexturePtrs_vl
            Mem.wl(a4, Mem.l(a2)); a2 += 4;            // move.l (a2)+,(a4)

            // complete the filename
            Mem.wb(LevelData.Lvl_WallFilenameN_vb, Mem.ub(a5)); a5 += 1; // move.b (a5)+,Lvl_WallFilenameN_vb
            r = FileIo.IO_LoadFileOptional(LevelData.Lvl_WallFilename_vb); // move.l #Lvl_WallFilename_vb,a0 ; jsr IO_LoadFileOptional

            Mem.wl(a3, FileIo.addr(r)); a3 += 4;       // move.l d0,(a3)+
            if (FileIo.addr(r) != 0) {                 // beq.s .done_this_wall
                // pointer was not null, so move it into Draw_WallTexturePtrs_vl
                Mem.wl(a4, FileIo.addr(r));            // move.l d0,(a4)
            }
            // .done_this_wall:
            a4 += 4;                                   // add.w #4,a4
            d2 = setw(d2, d2 - 1);                     // dbra d2,.do_wall
        } while ((short) d2 != -1);
        // movem.l (sp)+,... ; rts
    }

    /** Res_FreeLevelData */
    public static void Res_FreeLevelData() {
        if (Mem.l(Lvl_ErrataPtr_l) != 0) {             // tst.l Lvl_ErrataPtr_l ; beq.s .done_level_errata
            RES_FREEPTR(Lvl_ErrataPtr_l);              // RES_FREEPTR Lvl_ErrataPtr_l
        }
        // .done_level_errata:
        if (Mem.l(Lvl_ModPropertiesPtr_l) != 0) {      // tst.l Lvl_ModPropertiesPtr_l ; beq.s .done_level_properties
            RES_FREEPTR(Lvl_ModPropertiesPtr_l);       // RES_FREEPTR Lvl_ModPropertiesPtr_l
        }
        // .done_level_properties:
        // check for and free any custom floor overrides
        if (Mem.l(Draw_LevelFloorTexturesPtr_l) != 0) { // tst.l ... ; beq.s .done_floor_overrides
            RES_FREEPTR(Draw_LevelFloorTexturesPtr_l); // RES_FREEPTR Draw_LevelFloorTexturesPtr_l
            // reset the Draw_FloorTexturesPtr_l back to global set
            Mem.wl(Draw_FloorTexturesPtr_l, Mem.l(Draw_GlobalFloorTexturesPtr_l)); // move.l ...
        }
        // .done_floor_overrides:
        // movem.l d2/a2,-(sp)
        int d2 = NUM_WALL_TEXTURES - 1;                // moveq #NUM_WALL_TEXTURES-1,d2
        int a2 = Draw_LevelWallTexturePtrs_vl;         // move.l #Draw_LevelWallTexturePtrs_vl,a2
        res_FreeList(d2, a2);                          // bsr res_FreeList
        // movem.l (sp)+,d2/a2

        // .free_other:
        RES_FREEPTR(HiresData.Lvl_WalkLinksPtr_l);     // RES_FREEPTR Lvl_WalkLinksPtr_l
        RES_FREEPTR(HiresData.Lvl_FlyLinksPtr_l);      // RES_FREEPTR Lvl_FlyLinksPtr_l
        RES_FREEPTR(Lvl_GraphicsPtr_l);                // RES_FREEPTR Lvl_GraphicsPtr_l
        RES_FREEPTR(Lvl_ClipsPtr_l);                   // RES_FREEPTR Lvl_ClipsPtr_l
        RES_FREEPTR(HiresData.Lvl_MusicPtr_l);         // RES_FREEPTR Lvl_MusicPtr_l
        RES_FREEPTR(Lvl_DataPtr_l);                    // RES_FREEPTR Lvl_DataPtr_l

        // Edge PVS is managed by C code
        ZoneEdgePvs.Zone_FreeEdgePVS();                // CALLC Zone_FreeEdgePVS
        // rts
    }

    // *****************************************************************************
    // * Other
    // *****************************************************************************

    /** Res_ReleaseScreenMemory: rts */
    public static void Res_ReleaseScreenMemory() {
    }
}
