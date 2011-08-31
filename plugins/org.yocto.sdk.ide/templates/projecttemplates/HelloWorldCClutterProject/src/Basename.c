

#include <clutter/clutter.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define SCN_WIDTH  800
#define SCN_HEIGHT 480

const char *MAIN_STR = "$(message)";
const char *TEXT_FONT = "Courier Bold 170px";
const int FONT_SIZE = 170;
const guint32 colors[] = {0xFF0000FF/*red*/,    0x00FF00FF/*green*/,  
                          0x0000FFFF/*blue*/,   0xFFFF00FF/*yellow*/, 
                          0xFF00FFFF/*pink*/,   0x00FFFFFF/*teal*/,
                          0xFF8000FF/*orange*/, 0xFFFFFFFF/*white*/};
const int NUM_COLORS = 8;

// Return the next color in the colors[] array 
ClutterColor *get_next_color ()
{
    static int i=0;
    ClutterColor *cc = malloc (sizeof (ClutterColor));
    clutter_color_from_pixel (cc, colors[i++]);
    if (i > (NUM_COLORS-1)) {
        i=0;
    }
    return cc;
}

gulong get_rand_animation()
{
   int r = rand()%11;
   switch (r) {
   case 0: return CLUTTER_EASE_IN_OUT_QUAD;
   case 1: return CLUTTER_EASE_IN_OUT_CUBIC;
   case 2: return CLUTTER_EASE_IN_OUT_QUART;
   case 3: return CLUTTER_EASE_IN_OUT_QUINT;
   case 4: return CLUTTER_EASE_IN_OUT_SINE;
   case 5: return CLUTTER_EASE_IN_OUT_EXPO;
   case 6: return CLUTTER_EASE_IN_OUT_CIRC;
   case 7: return CLUTTER_EASE_IN_OUT_ELASTIC;
   case 8: return CLUTTER_EASE_IN_OUT_BACK;
   case 9: return CLUTTER_EASE_IN_OUT_BOUNCE;
   default: return CLUTTER_LINEAR;
   }
}

gint get_rand_option (gint opt1, gint opt2, gint opt3)
{
    gint num = rand()%3;
    if (num==0) {
        return opt1;
    } else if (num==1) {
        return opt2;
    } else {
        return opt3;
    }
}

// Each time the frame is painted, shade or lighten the coloring
void on_new_frame (ClutterTimeline *tl, 
                   gint frame_num, 
                   gpointer data)
{
    static gboolean get_darker = TRUE;

    ClutterActor *actor = CLUTTER_ACTOR(data);
    ClutterColor *orig_color = g_object_get_data (G_OBJECT (actor), "color");
    gint color_adj = GPOINTER_TO_INT(g_object_get_data(G_OBJECT(actor), 
                                                       "color_adjustment"));
    if (get_darker) {
        color_adj --;
        if (color_adj < -100) {
            color_adj = -100;
            get_darker = FALSE;
        }
    } else {
        color_adj += 1;
        if (color_adj > 0) {
            color_adj = 0;
            get_darker = TRUE;
        }
    }
    gdouble scale = 1.0+(0.005*(gdouble)color_adj);
    ClutterColor c;
    clutter_color_shade (orig_color, scale, &c);

    clutter_text_set_color (CLUTTER_TEXT(actor), &c); 
    g_object_set_data (G_OBJECT (actor), 
                       "color_adjustment", 
                       GINT_TO_POINTER(color_adj));
}

ClutterActor *create_new_character (char c, int xpos, int ypos) 
{
    ClutterActor *actor; 
    ClutterColor *color;
    char sz[2];
    sz[0] = c;
    sz[1] = '\0';

    color = get_next_color(); 
    actor = clutter_text_new_full (TEXT_FONT, sz, color);
    clutter_actor_set_position (actor, xpos, ypos);

    //Setup animation
    ClutterTimeline *timeline;
    timeline = clutter_timeline_new (60);    //num frames, fps
    clutter_timeline_set_loop (timeline, TRUE);

    //Create animation behavior (rotations)
    ClutterAlpha *alpha;
    ClutterBehaviour *behaviour;
    alpha = clutter_alpha_new_full (timeline, get_rand_animation());

    behaviour = clutter_behaviour_rotate_new (alpha, 
                                              get_rand_option(CLUTTER_X_AXIS,
                                                              CLUTTER_Y_AXIS,
                                                              CLUTTER_Z_AXIS),
                                              CLUTTER_ROTATE_CW,
                                              0.0, 360.0);
    clutter_behaviour_rotate_set_center (CLUTTER_BEHAVIOUR_ROTATE (behaviour),
                                         FONT_SIZE/2, FONT_SIZE/2, 0);

    clutter_behaviour_apply (behaviour, actor);
    g_signal_connect (timeline, "new-frame", G_CALLBACK (on_new_frame), actor);
    clutter_timeline_start (timeline);

    //save variables for later destruction
    g_object_set_data (G_OBJECT (actor), "color", color);
    g_object_set_data (G_OBJECT (actor), "color_adjustment", GINT_TO_POINTER(0));
    g_object_set_data (G_OBJECT (actor), "timeline", timeline);
    g_object_set_data (G_OBJECT (actor), "alpha", alpha);
    g_object_set_data (G_OBJECT (actor), "behaviour", behaviour);
    return actor;
}

static gboolean key_pressed_cb (ClutterStage *stage, 
                                ClutterEvent *event, gpointer data)
{
    if (event->type == CLUTTER_KEY_RELEASE) {
        ClutterEvent *kev = (ClutterEvent *) event;
        guint symb = clutter_event_get_key_symbol (kev);
        if (symb == CLUTTER_Escape || symb == CLUTTER_q) {
            clutter_main_quit ();
            return TRUE;
        }
    }
    return FALSE;
}

int main (int argc, char **argv)
{
    const ClutterColor scn_bkgd = {0x00,0x00,0xFF,0xFF};
    ClutterActor *stage;

    srand(time(NULL)); //for random colors

    clutter_init(&argc, &argv);

    //create stage, make fullscreen, set colors
    stage = clutter_stage_get_default();
    clutter_stage_set_fullscreen (CLUTTER_STAGE(stage), TRUE);

    gfloat scn_width = SCN_WIDTH, scn_height = SCN_HEIGHT;
    clutter_actor_get_size (stage, &scn_width, &scn_height);
    clutter_actor_set_size(stage, scn_width, scn_height);
    clutter_stage_set_color (CLUTTER_STAGE(stage), &scn_bkgd);
    clutter_stage_set_title(CLUTTER_STAGE(stage), "Clutter $(projectName)");

    //show MAIN_STR ('Hello World') and animate
    gint num_actors = 0;
    ClutterActor *actor;
    ClutterActor *arrActors[strlen(MAIN_STR)];
    gint xpos = (scn_width - (FONT_SIZE/3 * strlen(MAIN_STR)))/2;
    int i=0;
    for (i=0; i<strlen(MAIN_STR); i++) {
        if (MAIN_STR[i]==' ') {
            xpos += FONT_SIZE/3;
            continue;
        }
        actor = create_new_character (MAIN_STR[i], xpos, 
                                      (scn_height-FONT_SIZE)/2);
        arrActors[num_actors++] = actor;
        clutter_container_add_actor (CLUTTER_CONTAINER (stage), actor);
        xpos += FONT_SIZE/3;
    }

    clutter_actor_show_all (stage);

    // ESC exits
    g_signal_connect (stage, "key-release-event",
                      G_CALLBACK (key_pressed_cb), arrActors);
    clutter_main ();

    return 0;
}
