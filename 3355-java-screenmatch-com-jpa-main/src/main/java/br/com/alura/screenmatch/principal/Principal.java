package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio; //recebe o repositorio do application
    }

    public void exibeMenu() {
        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas  
                    4 - Buscar série por título     
                    5- Buscar série por ator   
                    6- Buscar top 5 séries  
                    7- Buscar séries por categoria 
                    8- Buscar séries pelo total de temporadas   
                    9- Filtrar séries por total de temporadas e avaliação
                    10- Buscar episodio por trecho
                    0 - Sair                                 
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    System.out.println("\n");
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    System.out.println("\n");
                    break;
                case 3:
                    listarSeriesBuscadas();
                    System.out.println("\n");
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTopSeries();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case  8:
                    buscarSeriePorTotalDeTemporadas();
                    break;
                case 9:
                    filtrarSeriesPorTemporadaEAvaliacao();
                    break;
                case 10:
                    buscarEpisodioPorTrecho();
                    break;
                case 11:
                    topEpisodiosPorSerie();
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    //OBS: O Optional é um tipo de atributo que serve para a ocasião que pode representar a presença ou ausência de um valor
    private void buscarSeriePorTitulo() {
        System.out.println("Escolha um série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()) {
            System.out.println("Dados da série: " + serieBusca.get());

        } else {
            System.out.println("Série não encontrada!");
        }

    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        //dadosSeries.add(dados);
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie() {
        listarSeriesBuscadas();
        System.out.println("Escolha uma série pelo nome");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serie = series.stream().
                filter(s -> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase()))
                .findFirst();
        if (serie.isPresent()) {
            var serieEncontrada = serie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }else {
            System.out.println("Série não encontrada");
        }
    }
    private void listarSeriesBuscadas(){
       series =  repositorio.findAll();// Vai no repositório e coleta todas as séries que já está cadastrada.
        series.stream().sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println); //Quando você imprime a classe Serie, você imprime o toString dela
    }

    private void buscarSeriePorAtor(){
        System.out.println("Qual o nome do ator para busca");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliação a partir de que valor");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Series em que " + nomeAtor + " trabalhou");
        seriesEncontradas.forEach(s-> System.out.println(s.getTitulo() + " | avaliação: " + s.getAvaliacao()));
    }

    private void buscarTopSeries(){
        List<Serie> seriesTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach( s->
                System.out.println(s.getTitulo() + " | Avaliação: " + s.getAvaliacao()));
    }

    private void  buscarSeriesPorCategoria(){
        System.out.println("Deseja buscar séries de que categoria/gênero?");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Séries da categoria " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarSeriePorTotalDeTemporadas(){
        System.out.println("Quantas temporadas no total deve ter a série?");
        var numeroTemporadas = leitura.nextInt();
        System.out.println("A partir de que valor de avaliação?");
        var avaliacao = leitura.nextDouble();

        List<Serie> SeriesPorTotalTemporada = repositorio.findByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(numeroTemporadas, avaliacao);
        SeriesPorTotalTemporada.forEach(s -> System.out.println(s.getTitulo() + " | Total de temporadas: " + s.getTotalTemporadas() + " | Total de avaliação: " + s.getAvaliacao()));
    }

    public void filtrarSeriesPorTemporadaEAvaliacao(){
        System.out.println("Filtrar séries até quantas temporadas?");
        var totalTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.println("Com avaliação a partir de que valor?");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        List<Serie> filtroSeries = repositorio.seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);
        filtroSeries.forEach(serie ->
                System.out.println(serie.getTitulo() + " - avaliação " + serie.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho(){
        System.out.println("Digite o trecho");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(e-> System.out.printf("Série %s temporada %s Episodio %s - %s \n",
                e.getSerie(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo()));
    }

    private void topEpisodiosPorSerie(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
            topEpisodios.forEach(e ->
                    System.out.printf("Série: %s Temporada %s - Episódio %s - %s Avaliação %s\n",
                            e.getSerie().getTitulo(), e.getTemporada(),
                            e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));
        }
    }
    private void buscarEpisodiosDepoisDeUmaData(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Digite o ano limite de lançamento");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = repositorio.episodiosPorSerieEAno(serie, anoLancamento);
            episodiosAno.forEach(System.out::println);
        }
    }

}
