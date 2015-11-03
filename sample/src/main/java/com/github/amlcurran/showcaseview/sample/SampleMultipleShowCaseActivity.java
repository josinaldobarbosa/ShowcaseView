package com.github.amlcurran.showcaseview.sample;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.TextPaint;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
                .withNewStyleShowcase()
                .setStyle(R.style.CustomShowcaseTheme2)
                //.singleShot(uniqueId)
                .setContentTitle("Titulo")
                .setContentTitlePaint(titlePaint)
                .setContentText("Descrição de teste de lerolero")
                .build();

        sv.setTitleTextAlignment(Layout.Alignment.ALIGN_CENTER);
        sv.setDetailTextAlignment(Layout.Alignment.ALIGN_CENTER);
        sv.forceTextPosition(ShowcaseView.BELOW_SHOWCASE);

        sv.setHideOnTouchOutside(true);






        Button btn1 = (Button) findViewById(R.id.newButton);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SampleMultipleShowCaseActivity.this, "Click!", Toast.LENGTH_SHORT).show();
            }
        });
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
