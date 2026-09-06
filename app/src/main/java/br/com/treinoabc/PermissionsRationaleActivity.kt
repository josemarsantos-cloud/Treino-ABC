package br.com.treinoabc

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class PermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24*density).toInt(), (40*density).toInt(), (24*density).toInt(), (24*density).toInt())
            setBackgroundColor(Color.rgb(7,16,31))
        }
        root.addView(TextView(this).apply {
            text = "Privacidade — Treino ABC"
            textSize = 24f
            setTextColor(Color.WHITE)
        }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(TextView(this).apply {
            text = "\nO Treino ABC solicita apenas permissão para gravar sessões de exercício no Health Connect. " +
                    "Os dados de treino permanecem armazenados localmente no aparelho e só são enviados ao Health Connect quando você autoriza. " +
                    "O aplicativo não vende nem compartilha esses dados com anunciantes ou terceiros.\n\n" +
                    "Você pode revogar a permissão a qualquer momento nas configurações do Health Connect."
            textSize = 17f
            setTextColor(Color.rgb(190,205,224))
            setLineSpacing(0f, 1.25f)
        }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
    }
}
