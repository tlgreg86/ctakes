#!python

from keras.models import Sequential, model_from_json
import numpy as np
import et_cleartk_io as ctk_io
import sys
import os.path
import pickle
from keras.preprocessing.sequence import pad_sequences

def main(args):
    if len(args) < 1:
        sys.stderr.write("Error - one required argument: <model directory>\n")
        sys.exit(-1)

    working_dir = args[0]

    int2label = {
        0:'none',
        1:'CONTAINS',
        2:'CONTAINS-1'
    }

    ctakes_root = '/Users/Dima/Loyola/Workspaces/cTakes/ctakes/'
    target_dir = 'ctakes-temporal/target/eval/thyme/train_and_test/event-time/'
    model_dir = ctakes_root + target_dir
    maxlen   = pickle.load(open(os.path.join(model_dir, "maxlen.p"), "rb"))
    alphabet = pickle.load(open(os.path.join(model_dir, "alphabet.p"), "rb"))
    model = model_from_json(open(os.path.join(model_dir, "model_0.json")).read())
    model.load_weights(os.path.join(model_dir, "model_0.h5"))

    while True:
        try:
            line = sys.stdin.readline().rstrip()
            if not line:
                break

            feats=[]
            for unigram in line.rstrip().split():
                if(alphabet.has_key(unigram)):
                    feats.append(alphabet[unigram])
                else:
                    feats.append(alphabet["none"])
                    
            if(len(feats) > maxlen):
                feats=feats[0:maxlen]
            test_x = pad_sequences([feats], maxlen=maxlen)

            X_dup = []
            X_dup.append(test_x)
            X_dup.append(test_x)
            X_dup.append(test_x)
            X_dup.append(test_x)

            out = model.predict(X_dup, batch_size=50)[0]

        except KeyboardInterrupt:
            sys.stderr.write("Caught keyboard interrupt\n")
            break

        if line == '':
            sys.stderr.write("Encountered empty string so exiting\n")
            break

        out_str = int2label[out.argmax()]
        print(out_str)
        sys.stdout.flush()

    sys.exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])
