package com.cursoandroid.instagram.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cursoandroid.instagram.R;
import com.cursoandroid.instagram.activity.EditarPerfilActivity;
import com.cursoandroid.instagram.activity.PerfilAmigoActivity;
import com.cursoandroid.instagram.adapter.AdapterGrid;
import com.cursoandroid.instagram.helper.ConfiguracaoFirebase;
import com.cursoandroid.instagram.helper.UsuarioFirebase;
import com.cursoandroid.instagram.model.Postagem;
import com.cursoandroid.instagram.model.Usuario;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class PerfilFragment extends Fragment {

    private ProgressBar progressBar;
    private CircleImageView imagePerfil;
    public GridView gridViewPerfil;
    private TextView textPublicacoes, textSeguidores, textSeguindo;
    private Button buttonEditarPerfil;
    private Usuario usuarioLogado;

    private  DatabaseReference firebaseRef;
    private DatabaseReference usuariosRef;
    private DatabaseReference usuarioLogadoRef;
    private ValueEventListener valueEventListenerPerfil;
    private DatabaseReference postagensUsuarioRef;
    private AdapterGrid adapterGrid;

    public PerfilFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_perfil, container, false);

        //Configurações iniciais
        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        firebaseRef = ConfiguracaoFirebase.getFirebase();
        usuariosRef = firebaseRef.child("usuarios");

        //Configurar referencia postagens usuario
        postagensUsuarioRef = ConfiguracaoFirebase.getFirebase()
                .child("postagens")
                .child( usuarioLogado.getId() );


        //Configurações dos componentes
        inicializarComponentes(view);



        //Abre edição do perfil
        buttonEditarPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), EditarPerfilActivity.class));
            }
        });

        //Inicializar image loader
        inicializarImageLoader();

        //Carrega as fotos das postagens de um usuário
        carregarFotosPostagem();

        return view;
    }

    public void carregarFotosPostagem() {

        //Recupera as fotos postadas pelo usuario
        postagensUsuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Configurar o tamanho do grid
                int tamanhoGrid = getResources().getDisplayMetrics().widthPixels;
                int tamanhoImagem = tamanhoGrid / 3;
                gridViewPerfil.setColumnWidth( tamanhoImagem );


                List<String> urlFotos = new ArrayList<>();
                for ( DataSnapshot ds: dataSnapshot.getChildren() ) {
                    Postagem postagem = ds.getValue( Postagem.class );
                    urlFotos.add( postagem.getCaminhoFoto() );
                }

                //Configura adapter
                adapterGrid = new AdapterGrid( getActivity() , R.layout.grid_postagem, urlFotos);
                gridViewPerfil.setAdapter( adapterGrid );
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void inicializarImageLoader() {

        ImageLoaderConfiguration config = new ImageLoaderConfiguration
                .Builder(getActivity())
                .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .diskCacheSize(50 * 1024 * 1024)
                .diskCacheFileCount(100)
                .diskCacheFileNameGenerator(new HashCodeFileNameGenerator())
                .build();
        ImageLoader.getInstance().init( config );
    }

    private void inicializarComponentes(View view){
        gridViewPerfil = view.findViewById(R.id.gridViewPerfil);
        progressBar = view.findViewById(R.id.progressBar);
        imagePerfil = view.findViewById(R.id.imagePerfil);
        textPublicacoes = view.findViewById(R.id.textPublicacoes);
        textSeguidores = view.findViewById(R.id.textSeguidores);
        textSeguindo = view.findViewById(R.id.textSeguindo);
        buttonEditarPerfil = view.findViewById(R.id.buttonAcaoPerfil);
    }

    private void recuperarDadosUsuarioLogado(){

        usuarioLogadoRef = usuariosRef.child( usuarioLogado.getId() );
        valueEventListenerPerfil = usuarioLogadoRef.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {


                            Usuario usuario = dataSnapshot.getValue( Usuario.class );

                                String postagens = String.valueOf( usuario.getPostagens() );
                                String seguindo = String.valueOf(usuario.getSeguindo());
                                String seguidores = String.valueOf(usuario.getSeguidores());

                                //Configura valores recuperados
                                textPublicacoes.setText( postagens );
                                textSeguidores.setText(seguidores);
                                textSeguindo.setText(seguindo);


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );

    }

    private void recuperarFotoUsuario() {

        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        //Recuperar foto do usuário
        String caminhoFoto = usuarioLogado.getCaminhoFoto();
        if( caminhoFoto != null ){
            Uri url = Uri.parse( caminhoFoto );
            Glide.with(getActivity())
                    .load( url )
                    .into( imagePerfil );
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        //Recuperar dados do usuário logado
        recuperarDadosUsuarioLogado();

        //Recuperar foto usuário
        recuperarFotoUsuario();


    }

    @Override
    public void onStop() {
        super.onStop();
        usuarioLogadoRef.removeEventListener( valueEventListenerPerfil );
    }
}
