#!/bin/bash

PORT=3000

# Função para encontrar processos usando a porta
find_port_process() {
    # Tenta lsof primeiro
    local pid=$(lsof -ti :$PORT 2>/dev/null)
    if [ ! -z "$pid" ]; then
        echo "$pid"
        return
    fi
    
    # Tenta fuser
    pid=$(fuser $PORT/tcp 2>/dev/null)
    if [ ! -z "$pid" ]; then
        echo "$pid"
        return
    fi
    
    # Tenta ss com sudo
    pid=$(sudo ss -tlnp 2>/dev/null | grep ":$PORT " | grep -oP 'pid=\K\d+' | head -1)
    if [ ! -z "$pid" ]; then
        echo "$pid"
        return
    fi
    
    # Verifica se a porta está em uso (sem PID)
    if ss -tln 2>/dev/null | grep -q ":$PORT "; then
        echo "unknown"
    fi
}

RESULT=$(find_port_process)

if [ ! -z "$RESULT" ]; then
    echo "⚠️  Porta $PORT já está em uso"
    
    if [ "$RESULT" != "unknown" ]; then
        echo "PID: $RESULT"
        ps -p $RESULT -o pid,command 2>/dev/null | tail -n 1
    else
        echo "Não foi possível identificar o PID (tente: sudo ss -tlnp | grep $PORT)"
    fi
    
    read -p "Deseja tentar matar o processo? (s/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        if [ "$RESULT" != "unknown" ]; then
            echo "Matando processo $RESULT e seus filhos..."
            pkill -P $RESULT 2>/dev/null
            kill -9 $RESULT 2>/dev/null
        else
            echo "Tentando matar processos na porta $PORT..."
            sudo fuser -k $PORT/tcp 2>/dev/null || sudo kill -9 $(sudo lsof -ti :$PORT) 2>/dev/null
        fi
        sleep 2
        
        RESULT2=$(find_port_process)
        if [ ! -z "$RESULT2" ]; then
            echo "⚠️  Porta ainda em uso, tentando com sudo..."
            sudo fuser -k -9 $PORT/tcp 2>/dev/null
            sleep 1
        fi
        
        echo "✅ Processo finalizado"
    else
        echo "❌ Operação cancelada"
        exit 1
    fi
fi

cd apps/frontend && npm run dev
