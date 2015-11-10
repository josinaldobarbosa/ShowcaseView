package com.github.amlcurran.showcaseview.sample;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

/**
 * Created by josinaldo on 02/11/15.
 */
public class SampleMultipleShowCaseActivity extends AppCompatActivity {

    TabLayout tbLayout;
    long uniqueId = 599;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_multiple_showcase);

        tbLayout = (TabLayout) findViewById(R.id.tab);

        View tab1 = addTabsWithContent("Tab1");
        View tab2 = addTabsWithContent("Tab2");
        View tab3 = addTabsWithContent("Tab3");

        // title paint
        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(getResources().getDimension(R.dimen.abc_text_size_headline_material));
        titlePaint.setTypeface(Typeface.createFromAsset(getAssets(), "RobotoSlab-Regular.ttf"));
        //

        final ShowcaseView sv = new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(tab1), new ViewTarget(tab2), new ViewTarget(tab3))
                .withMaterialShowcase()
                .setStyle(R.style.CustomShowcaseTheme2)
                //.singleShot(uniqueId)
                .setContentTitlePaint(titlePaint)
                .setContentText("Navegue aqui para conhecer as seções e descubra o que está bombando dentro de cada uma.")
                .setCompensationTextPosition(0, 220)
                .replaceEndButton(getShowCaseCloseView())
                .build();

        sv.setBlocksTouches(true);

        sv.setTitleTextAlignment(Layout.Alignment.ALIGN_CENTER);
        sv.setDetailTextAlignment(Layout.Alignment.ALIGN_CENTER);
        sv.forceTextPosition(ShowcaseView.BELOW_SHOWCASE);

        sv.setButtonPosition(getLayoutParamsButtonClose());

        sv.setHideOnTouchOutside(true);


        Button btn1 = (Button) findViewById(R.id.newButton);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SampleMultipleShowCaseActivity.this, "Click!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View getShowCaseCloseView() {
        ImageView imageClose = new ImageView(this);
        imageClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        return imageClose;
    }

    private RelativeLayout.LayoutParams getLayoutParamsButtonClose() {
        int margin = 16;
        int moveTopDown = 21;

        margin = ((Number) (getResources().getDisplayMetrics().density * margin)).intValue();
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        int marginTop = margin + (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                moveTopDown,
                getResources().getDisplayMetrics());

        lps.setMargins(margin, marginTop, margin, margin);
        return lps;
    }

    private View addTabsWithContent(String texto) {
        LinearLayout ll = new LinearLayout(this);
        TextView tx = new TextView(this);
        tx.setText(texto);
        ll.addView(tx);

        tbLayout.addTab(tbLayout.newTab().setCustomView(ll));

        return ll;
    }

}
