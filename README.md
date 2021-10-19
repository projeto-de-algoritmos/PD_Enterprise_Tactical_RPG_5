# Enterprise Tactical RPG 5

**Número da Lista**: 25<br>
**Conteúdo da Disciplina**: PD<br>

## Alunos

| Matrícula | Aluno |
| ---------- | -- |
| 15/0058462 |  Davi Antônio da Silva Santos |
| 18/0100840 |  Gabriel Azevedo Batalha |

## Sobre

Um jogo cujo objetivo é sobreviver o maior número de turnos. É uma evolução do
projeto de Dividir e Conquistar, agora permitindo habilitar sons simples e com
adição de inimigos *WIS* (*Weighted Interval Scheduling*).

## Screenshots


![Menu](https://i.imgur.com/XTVMAaS.png)
Menu

![Jogo em execução em mapa 20x20](https://i.imgur.com/BxXeAoo.png)
Jogo em execução em mapa 20x20

![Jogo em execução em mapa 30x30](https://i.imgur.com/xAU7Hec.png)
Jogo em execução em mapa 30x30

## Instalação 
**Linguagem**: Java<br>
**Framework**: Swing<br>

### Requisitos

- Java JRE 11 ou superior.
  - JDK 11 ou superior exigido para compilar ou desenvolver
- Computador com *mouse*.

## Uso 

Clone o repositório para compilar o projeto ou baixe somente o `.jar` e os assets disponíveis
nas [releases](https://github.com/projeto-de-algoritmos/PD_Enterprise_Tactical_RPG_5/releases)

Para executar o programa, use
```
java -jar EnterpriseTacticalRPG5.jar
```

Caso opte por utilizar o jar, é preciso executá-lo no mesmo diretório que a pasta assets.

O jogo é controlado pelo *mouse*. Há a possibilidade de escolha do tamanho do
mapa até no máximo 30x30 e no mínimo 16x16 posições.

O jogador é um ponto azul na tela e deve fugir dos pontos vermelhos inimigos.
Os quadrados vermelhos são inimigos que seguem o jogador independentemente,
traçando o caminho de menor custo usando o algoritmo de Dijkstra.

Já os círculos vermelhos são um exército que persegue o jogador usando um
algoritmo ganancioso. O exército ganancioso tem um número fixo de casas que
pode movimentar, e escolhe o inimigo que chega mais próximo do jogador no menor
número de casas.

Os semicírculos vermelhos são um exército que persegue o jogador escolhendo
sempre a unidade na mediana dos custos para chegar até o jogador. Esses
inimigos seguem a mesma limitação de custo de movimento por unidade presente
nos inimigos comuns.

Os "quartos de círculos" são um exército que persegue o jogador usando o
algoritmo do agendamento de intervalos com pesos. Cada caminho até o alvo, no
caso o jogador, representa um evento com início e fim, sendo este último
determinado pelo número de rodadas que o inimigo analisado demoraria para
alcançar o alvo. O peso de cada tarefa é a distância em casas que será
percorrida até o alvo, facilitando para o jogador.

De acordo com o algoritmo, as tarefas incompatíveis de menor valor tendem a não
ser escolhidas, por isso é comum ver dois inimigos não se movendo. Nesse caso,
eles alcançam o jogador ao mesmo tempo e possuem menor valor para o algoritmo.

As áreas são coloridas conforme os custos para atravessá-las. Regiões verdes
possuem o custo mais baixo, e quanto mais amarelo, mais alto o custo. Regiões
pretas são intransponíveis.

A partida termina quando o jogador é alcançado por qualquer um dos inimigos ou
quando não há movimentos válidos restantes.

## Desenvolvimento

Ao importar o projeto em sua IDE talvez seja necessário retirar o `.jar` gerado
do caminho do projeto. É possível que a IDE tente usar as classes empacotadas
no lugar das que estão definidas no código fonte.

## Outros

Agora é possível escolher se o jogo reproduzirá sons.