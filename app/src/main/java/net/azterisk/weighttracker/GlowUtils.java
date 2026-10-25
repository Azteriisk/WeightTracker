package net.azterisk.weighttracker;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Helper utility for applying custom window flags, text gradient shaders,
 * and title shadow styling across activities.
 */
public class GlowUtils {

    /**
     * Configures transparent status and navigation bars for full-screen layout rendering.
     */
    public static void enableEdgeToEdge(AppCompatActivity activity) {
        if (activity == null) return;

        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * Applies a horizontal teal-to-green linear gradient shader and shadow to a TextView.
     */
    public static void applyNeonGlow(TextView textView) {
        if (textView == null) return;

        textView.post(() -> {
            float width = textView.getWidth();
            if (width <= 0) {
                width = textView.getPaint().measureText(textView.getText().toString());
            }
            if (width <= 0) width = 300f;

            Shader textShader = new LinearGradient(
                    0, 0, width, 0,
                    new int[]{Color.parseColor("#449C92"), Color.parseColor("#64A35A")},
                    null, Shader.TileMode.CLAMP
            );
            textView.getPaint().setShader(textShader);

            textView.setShadowLayer(28f, 0f, 0f, Color.parseColor("#703C9187"));
            textView.invalidate();
        });
    }

    /**
     * Applies a subtle gradient shader to sub-titles or secondary headers.
     */
    public static void applySubtleGlow(TextView textView) {
        if (textView == null) return;

        textView.post(() -> {
            float width = textView.getWidth();
            if (width <= 0) {
                width = textView.getPaint().measureText(textView.getText().toString());
            }
            if (width <= 0) width = 200f;

            Shader textShader = new LinearGradient(
                    0, 0, width, 0,
                    new int[]{Color.parseColor("#4DA399"), Color.parseColor("#6AA85E")},
                    null, Shader.TileMode.CLAMP
            );
            textView.getPaint().setShader(textShader);
            textView.invalidate();
        });
    }
}
