package ai_server_cafe.replay.clip.wyscout;

public class WyscoutData {

    /**
     * match id
     */
    public int match_id;

    /**
     * While a team is in possession of the ball. <p>
     * For example, if the Blue team has possession from the start of the match, the value is 0. <p>
     * When the Yellow team gains possession, it changes to 1.
     */
    public int poss_id;

    /**
     * team id
     */
    public int team;

    /**
     * 0 = Blue team, 1 = Yellow team
     */
    public int home_team;

    /**
     * Action name mapping: <p>
     * ------------------------- <p>
     * short_pass  : short pass <p>
     * game_over   : end game <p>
     * _           : none <p>
     */
    public String action;

    /**
     * Action success (1 = success, 0 = failure)
     */
    public int success;

    /**
     * Whether the ball went into the goal.
     * Then this value is 1
     */
    public int goal;

    /**
     * blue team score
     */
    public int home_score;

    /**
     * yellow team score
     */
    public int away_score;

    /**
     * the score between blue team and yellow team
     */
    public int goal_diff;

    /**
     * 1st half or 2nd half
     */
    public int Period;

    /**
     * minutes into the half
     */
    public int Minute;

    /**
     * seconds into the half
     */
    public long Second;

    /**
     * seconds into match
     */
    public long seconds;

    /**
     * seconds from previous action to current action
     * initial value is 0
     */
    public long delta_T;

    /**
     * start x-coordinate of the action
     */
    public double start_x;

    /**
     * start y-coordinate of the action
     */
    public double start_y;

    /**
     * x-coordinate difference from the previous action to the current action
     * initial value is 0
     */
    public double deltaX;

    /**
     * y-coordinate difference from the previous action to the current action
     * initial value is 0
     */
    public double deltaY;

    /**
     * distance from previous action point to current action point
     * initial value is 0
     */
    public double distance;

    /**
     * distance from action point to goal
     * initial value is 0
     */
    public double dist2goal;

    /**
     * angle from action point to goal
     */
    public double angle2goal;
}
