package br.com.proximojogo.proximojogo.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import br.com.proximojogo.proximojogo.MainActivity;
import br.com.proximojogo.proximojogo.R;
import br.com.proximojogo.proximojogo.entity.AgendaDO;
import br.com.proximojogo.proximojogo.ordenacao.OrdenaEstatiscaJogosPorData;
import br.com.proximojogo.proximojogo.ordenacao.OrdenaEventoTimeData;
import br.com.proximojogo.proximojogo.ui.ListaEventosPassadosAgenda;
import br.com.proximojogo.proximojogo.utils.EstatisticaDeJogos;
import br.com.proximojogo.proximojogo.utils.FormatarData;
import br.com.proximojogo.proximojogo.utils.GetUser;

/**
 * Created by Ale on 28/02/2018.
 */

public class VerificaEventosService extends JobService {
//    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child("agendas" + "/" + GetUser.getUserLogado());
    private DatabaseReference estatisticaReference = FirebaseDatabase.getInstance().getReference("estatistica-jogos-list");
    private ArrayList<AgendaDO> eventos = new ArrayList<>();
    private ArrayList<EstatisticaDeJogos> estatisticas = new ArrayList<>();

    @Override
    public boolean onStartJob(JobParameters job) {
//        retrieve();
        buscaEventos();
        return false;

    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    private void postNotif(List<EstatisticaDeJogos> estatisticaDeJogosList) {
        Intent intent = new Intent(this, ListaEventosPassadosAgenda.class);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.soccer_player_icon)
                .setTicker("Há confrontos com mais de 30 dias!")
                .setContentTitle("Há " + estatisticaDeJogosList.size()+" jogos com mais de 30 dias!")
                .setContentText("Vai Audax! Vamos ganhar!")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        int id = 1;

        NotificationManager notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifyManager.notify(id, builder.build());
    }


    public void buscaEventos() {

        LocalDate hoje = new LocalDate(new Date());
        LocalDate trintaDiasAtras = hoje.minusDays(30);
        Date date = trintaDiasAtras.toDate();
        Query query1 = estatisticaReference.orderByChild("dataUltimoComfronto").endAt(date.getTime());
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fetchData(dataSnapshot);//Passar os dados para a interface grafica
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Se ocorrer um erro
            }
        });
    }
    private void fetchData(DataSnapshot dataSnapshot) {
        eventos.clear();
        for (DataSnapshot ds : dataSnapshot.getChildren()) {
            EstatisticaDeJogos estatisticaDeJogos = ds.getValue(EstatisticaDeJogos.class);
            estatisticas.add(estatisticaDeJogos);
        }
        for(EstatisticaDeJogos a: estatisticas){
            Log.i("EVENTOS", a.getTime2()+" Data: "+FormatarData.getDf().format(a.getDataUltimoComfronto()));
        }
        postNotif(estatisticas);
    }
}
